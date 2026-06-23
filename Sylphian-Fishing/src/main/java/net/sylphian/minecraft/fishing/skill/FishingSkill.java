package net.sylphian.minecraft.fishing.skill;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.SylphianFishing;
import net.sylphian.minecraft.fishing.skill.ability.CatchMomentum;
import net.sylphian.minecraft.fishing.skill.ability.DoubleHaul;
import net.sylphian.minecraft.fishing.skill.ability.FishersFrenzy;
import net.sylphian.minecraft.fishing.skill.ability.LineMastery;
import net.sylphian.minecraft.fishing.skill.ability.MasterAngler;
import net.sylphian.minecraft.fishing.skill.ability.PatientAngler;
import net.sylphian.minecraft.fishing.skill.ability.SteadyCurrent;
import net.sylphian.minecraft.skills.api.SkillsAPI;
import net.sylphian.minecraft.skills.service.ActiveBuffTracker;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.AbstractSkill;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Fishing skill contributed to Sylphian-Skills by Sylphian-Fishing.
 *
 * <p>Acts as the event coordinator: routes Bukkit events to the appropriate
 * ability logic and manages shared per-player state. Abilities are registered
 * in {@link #addAbility} order so the framework can surface them in order.</p>
 *
 * <p>All state maps are accessed on the main thread only.</p>
 */
public final class FishingSkill extends AbstractSkill {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** PDC key stamped on every Sylphian fish by LootService. */
    private static final NamespacedKey FISH_KEY = new NamespacedKey("sylphian-fishing", "item_id");

    private final SylphianFishing plugin;
    private volatile FishingSkillConfig config;

    // Abilities - created in constructor, injected into addAbility() in unlock order.
    private final PatientAngler  patientAngler;
    private final LineMastery    lineMastery;
    private final DoubleHaul     doubleHaul;
    private final SteadyCurrent  steadyCurrent;
    private final FishersFrenzy  fishersFrenzy;
    private final MasterAngler   masterAngler;

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

        patientAngler = new PatientAngler();
        lineMastery   = new LineMastery();
        doubleHaul    = new DoubleHaul();
        steadyCurrent = new SteadyCurrent();
        fishersFrenzy = new FishersFrenzy();
        masterAngler  = new MasterAngler();

        addAbility(patientAngler);
        addAbility(lineMastery);
        addAbility(doubleHaul);
        addAbility(steadyCurrent);
        addAbility(fishersFrenzy);
        addAbility(masterAngler);
    }

    /**
     * Reads the initial config snapshot then delegates listener registration
     * to {@link AbstractSkill}, which stores the API reference and registers
     * {@code this} as a Bukkit listener.
     */
    @Override
    public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
        this.config = FishingSkillConfig.from(plugin.getConfig());
        super.registerListeners(owningPlugin, api);
    }

    /**
     * Swaps the config snapshot on a Fishing plugin reload.
     */
    @Override
    public void reload() {
        this.config = FishingSkillConfig.from(plugin.getConfig());
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

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
     * On cast: tracks the hook, applies Patient Angler if pending, then
     * stacks all passive timer reductions on top.
     */
    private void handleCast(PlayerFishEvent event, Player player, UUID uuid) {
        activelyCasting.add(uuid);

        FishingSkillConfig cfg = config;
        int level = skillsApi.getCachedLevel(uuid, "fishing");

        patientAngler.applyOnCast(event.getHook(), uuid, cfg, patientAnglerPending);
        applyTimerReduction(event.getHook(), uuid, level, cfg);
    }

    /**
     * On catch: removes from casting set, updates Steady Current momentum,
     * applies Double Haul if pending, and awards XP for Sylphian fish.
     */
    private void handleCatch(PlayerFishEvent event, Player player, UUID uuid) {
        activelyCasting.remove(uuid);

        if (!(event.getCaught() instanceof Item caughtItem)) return;

        ItemStack caught = caughtItem.getItemStack();
        if (!isSylphianFish(caught)) return;

        FishingSkillConfig cfg = config;
        int level = skillsApi.getCachedLevel(uuid, "fishing");

        if (level >= steadyCurrent.unlockLevel()) {
            steadyCurrent.updateMomentum(player, uuid, caughtItem.getLocation(), cfg, momentum);
        }
        if (level >= doubleHaul.unlockLevel()) {
            doubleHaul.applyOnCatch(player, uuid, caught, plugin, doubleHaulPending);
        }

        skillsApi.awardXP(player, "fishing", computeXp(uuid, level, cfg));
    }

    /**
     * Sneaking and right-clicking with a fishing rod while not casting activates
     * the highest unlocked, off-cooldown active perk.
     */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> { /* proceed */ }
            default -> { return; }
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.FISHING_ROD) return;

        UUID uuid = player.getUniqueId();
        if (activelyCasting.contains(uuid)) return; // hook is already in the water

        int level = skillsApi.getCachedLevel(uuid, "fishing");
        if (level < patientAngler.unlockLevel()) return; // no active perks yet

        event.setCancelled(true); // prevent the cast
        activateNextPerk(player, uuid, level, config);
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

    // -------------------------------------------------------------------------
    // Active perk activation
    // -------------------------------------------------------------------------

    /**
     * Activates the highest-priority active perk that is unlocked and off cooldown.
     * Priority: Fisher's Frenzy (25) > Double Haul (15) > Patient Angler (5).
     */
    private void activateNextPerk(Player player, UUID uuid, int level, FishingSkillConfig cfg) {
        CooldownManager  cd    = skillsApi.getCooldownManager();
        ActiveBuffTracker buffs = skillsApi.getActiveBuffTracker();

        if (level >= fishersFrenzy.unlockLevel()
                && !fishersFrenzy.isActive(uuid, buffs)
                && !cd.isOnCooldown(uuid, FishersFrenzy.COOLDOWN_ID)) {
            fishersFrenzy.activate(player, uuid, cfg, cd, buffs, plugin);
            return;
        }
        if (level >= doubleHaul.unlockLevel()
                && !doubleHaulPending.contains(uuid)
                && !cd.isOnCooldown(uuid, DoubleHaul.COOLDOWN_ID)) {
            doubleHaul.activate(player, uuid, cfg, cd, doubleHaulPending);
            return;
        }
        if (level >= patientAngler.unlockLevel()
                && !patientAnglerPending.contains(uuid)
                && !cd.isOnCooldown(uuid, PatientAngler.COOLDOWN_ID)) {
            patientAngler.activate(player, uuid, cfg, cd, patientAnglerPending);
            return;
        }

        showCooldownStatus(player, uuid, level, cd, buffs);
    }

    /**
     * Shows the most relevant cooldown or pending status when no perk can be activated.
     */
    private void showCooldownStatus(Player player, UUID uuid, int level,
                                    CooldownManager cd, ActiveBuffTracker buffs) {
        if (level >= fishersFrenzy.unlockLevel()) {
            if (fishersFrenzy.isActive(uuid, buffs)) {
                player.sendActionBar(MINI.deserialize("<gold>Fisher's Frenzy <white>is already active!"));
                return;
            }
            long s = cd.getRemainingSeconds(uuid, FishersFrenzy.COOLDOWN_ID);
            if (s > 0) {
                player.sendActionBar(MINI.deserialize("<red>Fisher's Frenzy: <white>" + s + "s remaining."));
                return;
            }
        }
        if (level >= doubleHaul.unlockLevel()) {
            if (doubleHaulPending.contains(uuid)) {
                player.sendActionBar(MINI.deserialize(
                        "<yellow>Double Haul <white>is already pending your next catch."));
                return;
            }
            long s = cd.getRemainingSeconds(uuid, DoubleHaul.COOLDOWN_ID);
            if (s > 0) {
                player.sendActionBar(MINI.deserialize("<red>Double Haul: <white>" + s + "s remaining."));
                return;
            }
        }
        if (level >= patientAngler.unlockLevel()) {
            if (patientAnglerPending.contains(uuid)) {
                player.sendActionBar(MINI.deserialize(
                        "<yellow>Patient Angler <white>is already pending your next cast."));
                return;
            }
            long s = cd.getRemainingSeconds(uuid, PatientAngler.COOLDOWN_ID);
            if (s > 0) {
                player.sendActionBar(MINI.deserialize("<red>Patient Angler: <white>" + s + "s remaining."));
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    /**
     * Scales down the hook's wait times by the combined passive reduction.
     * Reductions from Line Mastery, Steady Current, Master Angler, and Fisher's
     * Frenzy stack additively, capped at 90%.
     */
    private void applyTimerReduction(FishHook hook, UUID uuid, int level, FishingSkillConfig cfg) {
        double reduction = 0.0;

        if (level >= lineMastery.unlockLevel())   reduction += lineMastery.reductionFraction(cfg);
        if (level >= steadyCurrent.unlockLevel())  reduction += steadyCurrent.reductionFraction(uuid, cfg, momentum);
        if (level >= masterAngler.unlockLevel())   reduction += masterAngler.reductionFraction(cfg);
        reduction += fishersFrenzy.reductionFraction(uuid, skillsApi.getActiveBuffTracker(), cfg);

        if (reduction <= 0.0) return;
        reduction = Math.min(0.90, reduction);

        int newMin = Math.max(20, (int) (hook.getMinWaitTime() * (1.0 - reduction)));
        int newMax = Math.max(newMin, (int) (hook.getMaxWaitTime() * (1.0 - reduction)));
        hook.setMinWaitTime(newMin);
        hook.setMaxWaitTime(newMax);
    }

    /**
     * Computes the XP to award for a catch, applying Master Angler and Frenzy multipliers.
     *
     * @param uuid  the player's UUID
     * @param level the player's current fishing level
     * @param cfg   the current config snapshot
     * @return XP to award, at least 1
     */
    private long computeXp(UUID uuid, int level, FishingSkillConfig cfg) {
        double multiplier = 1.0;
        if (level >= masterAngler.unlockLevel()) multiplier *= masterAngler.xpMultiplier(cfg);
        multiplier *= fishersFrenzy.xpMultiplier(uuid, skillsApi.getActiveBuffTracker());
        return Math.max(1L, (long) (cfg.xpPerCatch() * multiplier));
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
