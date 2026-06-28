package net.sylphian.minecraft.fishing.skill;

import net.sylphian.minecraft.fishing.SylphianFishing;
import net.sylphian.minecraft.fishing.skill.ability.CatchMomentum;
import net.sylphian.minecraft.fishing.skill.ability.DoubleHaul;
import net.sylphian.minecraft.fishing.skill.ability.FishersFrenzy;
import net.sylphian.minecraft.fishing.skill.ability.LineMastery;
import net.sylphian.minecraft.fishing.skill.ability.MasterAngler;
import net.sylphian.minecraft.fishing.skill.ability.PatientAngler;
import net.sylphian.minecraft.fishing.skill.ability.SteadyCurrent;
import net.sylphian.minecraft.fishing.skill.trigger.FishCastTrigger;
import net.sylphian.minecraft.fishing.skill.trigger.FishCatchTrigger;
import net.sylphian.minecraft.skills.api.SkillsAPI;
import net.sylphian.minecraft.skills.skill.AbstractSkill;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The Fishing skill contributed to Sylphian-Skills by Sylphian-Fishing.
 *
 * <p>Acts as the event coordinator: routes Bukkit events to the appropriate
 * ability logic and manages shared per-player state. Abilities are constructed
 * with injected dependencies in {@link #registerListeners} and added in
 * unlock-level order so the framework surfaces them correctly.</p>
 *
 * <p>Active ability selection and activation (sneak + right-click)
 * is handled generically by {@code ActiveAbilityCoordinator} in Sylphian-Skills.
 * This class only implements {@link #activationMaterial()} and {@link #canInteract}
 * to participate in that framework.</p>
 *
 * <p>All state maps are accessed on the main thread only.</p>
 */
public final class FishingSkill extends AbstractSkill {

    /** PDC key stamped on every Sylphian fish by LootService. */
    private static final NamespacedKey FISH_KEY = new NamespacedKey("sylphian-fishing", "item_id");

    private final SylphianFishing plugin;
    private volatile FishingSkillConfig config;

    // Abilities — assigned in registerListeners once API deps are available.
    private PatientAngler patientAngler;
    private LineMastery   lineMastery;
    private DoubleHaul    doubleHaul;
    private SteadyCurrent steadyCurrent;
    private FishersFrenzy fishersFrenzy;
    private MasterAngler  masterAngler;

    // Main-thread state shared across abilities.
    /** Players who currently have a hook in the water. */
    private final Set<UUID> activelyCasting      = new HashSet<>();
    /** Players whose next cast will be fast (Patient Angler pending). */
    private final Set<UUID> patientAnglerPending = new HashSet<>();
    /** Players whose next catch will be duplicated (Double Haul pending). */
    private final Set<UUID> doubleHaulPending    = new HashSet<>();
    /** Steady Current streak state per player. */
    private final Map<UUID, CatchMomentum> momentum = new HashMap<>();

    /**
     * @param plugin the owning Fishing plugin instance
     */
    public FishingSkill(SylphianFishing plugin) {
        super("fishing", "Fishing");
        this.plugin = plugin;
    }

    /**
     * Constructs abilities with their injected dependencies, registers them in
     * unlock-level order, reads the initial config snapshot, and registers this
     * skill as a Bukkit listener via the parent.
     */
    @Override
    public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
        this.config = FishingSkillConfig.from(plugin.getConfig());

        patientAngler = new PatientAngler(() -> config, api.getCooldownManager(), patientAnglerPending);
        lineMastery   = new LineMastery(() -> config);
        doubleHaul    = new DoubleHaul(() -> config, api.getCooldownManager(), doubleHaulPending, owningPlugin);
        steadyCurrent = new SteadyCurrent(() -> config, momentum);
        fishersFrenzy = new FishersFrenzy(() -> config, api.getCooldownManager(), api.getActiveBuffTracker(), owningPlugin);
        masterAngler  = new MasterAngler(() -> config);

        addAbility(patientAngler);
        addAbility(lineMastery);
        addAbility(doubleHaul);
        addAbility(steadyCurrent);
        addAbility(fishersFrenzy);
        addAbility(masterAngler);

        super.registerListeners(owningPlugin, api);
    }

    /**
     * Swaps the config snapshot on a Fishing plugin reload.
     */
    @Override
    public void reload() {
        this.config = FishingSkillConfig.from(plugin.getConfig());
    }

    /**
     * The item a player must hold for the {@code ActiveAbilityCoordinator} to
     * intercept sneak-right-click gestures.
     */
    @Override
    public Optional<Material> activationMaterial() {
        return Optional.of(Material.FISHING_ROD);
    }

    /**
     * Suppresses ability activation while the hook is in the water so the player
     * can reel in normally with a right-click.
     */
    @Override
    public boolean canInteract(Player player, UUID uuid) {
        return !activelyCasting.contains(uuid);
    }

    /**
     * Routes fishing state changes to the appropriate handler.
     * Runs at HIGHEST so passive timer reductions are applied after
     * BiteTimerService (NORMAL) has already set the hook's wait times.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        switch (event.getState()) {
            case FISHING     -> handleCast(event, player, uuid);
            case CAUGHT_FISH -> handleCatch(event, player, uuid);
            // All terminal states: the hook is no longer in the water.
            default          -> activelyCasting.remove(uuid);
        }
    }

    /**
     * On cast: tracks the hook, applies Patient Angler if pending, then fires
     * passive cast triggers to accumulate timer reductions before applying them.
     */
    private void handleCast(PlayerFishEvent event, Player player, UUID uuid) {
        activelyCasting.add(uuid);
        patientAngler.applyOnCast(event.getHook(), uuid);

        FishCastTrigger castTrigger = new FishCastTrigger(event.getHook());
        firePassives(castTrigger, player, uuid);
        castTrigger.addReduction(fishersFrenzy.reductionFraction(uuid));
        castTrigger.applyToHook();
    }

    /**
     * On catch: removes from casting set, fires passive catch triggers to update
     * momentum and accumulate XP multipliers, then applies Double Haul and awards XP.
     */
    private void handleCatch(PlayerFishEvent event, Player player, UUID uuid) {
        activelyCasting.remove(uuid);

        if (!(event.getCaught() instanceof Item caughtItem)) return;

        ItemStack caught = caughtItem.getItemStack();
        if (!isSylphianFish(caught)) return;

        FishCatchTrigger catchTrigger = new FishCatchTrigger(caught, caughtItem.getLocation());
        firePassives(catchTrigger, player, uuid);
        catchTrigger.multiplyXp(fishersFrenzy.xpMultiplier(uuid));

        doubleHaul.applyOnCatch(player, uuid, caught);

        long xp = Math.max(1L, (long) (config.xpPerCatch() * catchTrigger.xpMultiplier()));
        skillsApi.awardXP(player, "fishing", xp);
    }

    /** Clears all per-player state on disconnect. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activelyCasting.remove(uuid);
        patientAnglerPending.remove(uuid);
        doubleHaulPending.remove(uuid);
        momentum.remove(uuid);
    }

    /**
     * Resets Steady Current momentum on a world change or a distant teleport
     * (more than 10 blocks), since the player has left their fishing spot.
     */
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;

        boolean worldChanged = !from.getWorld().equals(to.getWorld());
        boolean distantMove  = !worldChanged && from.distanceSquared(to) > 100;

        if (worldChanged || distantMove) {
            momentum.remove(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Returns {@code true} if the item carries the {@code sylphian-fishing:item_id} PDC key,
     * indicating it was produced by Sylphian-Fishing's loot system.
     */
    private static boolean isSylphianFish(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                   .has(FISH_KEY, PersistentDataType.STRING);
    }
}
