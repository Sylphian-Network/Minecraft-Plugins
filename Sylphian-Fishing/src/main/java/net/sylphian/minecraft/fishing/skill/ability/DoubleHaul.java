package net.sylphian.minecraft.fishing.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActivationResult;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active ability unlocked at level 15.
 *
 * <p>When activated, the player's next catch yields a second copy of the item,
 * delivered one tick after the original to ensure proper inventory handling.</p>
 */
public final class DoubleHaul implements ActiveAbility {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Cooldown key used with {@link CooldownManager}. */
    public static final String COOLDOWN_ID = "fishing:double-haul";

    private final Supplier<FishingSkillConfig> config;
    private final CooldownManager cooldownManager;
    private final Set<UUID> pendingSet;
    private final Plugin plugin;

    /**
     * @param config          supplier for the current config snapshot
     * @param cooldownManager the shared cooldown manager
     * @param pendingSet      the set tracking whose next catch is duplicated
     * @param plugin          the owning plugin, used to schedule the delayed item delivery
     */
    public DoubleHaul(Supplier<FishingSkillConfig> config,
                      CooldownManager cooldownManager,
                      Set<UUID> pendingSet,
                      Plugin plugin) {
        this.config          = config;
        this.cooldownManager = cooldownManager;
        this.pendingSet      = pendingSet;
        this.plugin          = plugin;
    }
    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Double Haul"; }
    @Override public String description() { return "Your next catch yields a second copy of the item."; }
    @Override public int    unlockLevel() { return 15; }

    /**
     * Called by the framework when the player activates this ability.
     * Checks cooldown and pending state, then marks the next catch for duplication.
     */
    @Override
    public ActivationResult onActivate(Player player, UUID uuid) {
        if (pendingSet.contains(uuid)) {
            player.sendActionBar(MINI.deserialize(
                    "<yellow>Double Haul <white>is already pending your next catch."));
            return ActivationResult.blocked();
        }
        long remaining = cooldownManager.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize(
                    "<red>Double Haul: <white>" + (remaining / 1000) + "s remaining."));
            return ActivationResult.blocked();
        }
        FishingSkillConfig cfg = config.get();
        pendingSet.add(uuid);
        cooldownManager.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.doubleHaulCooldownSeconds()));
        player.sendActionBar(MINI.deserialize(
                "<aqua>Double Haul <white>ready! Your next catch will be duplicated."));
        return ActivationResult.used("primed: next catch duplicated");
    }

    /**
     * Short MiniMessage status string shown as item lore in the ability selection GUI.
     */
    @Override
    public String selectionStatus(UUID uuid) {
        if (pendingSet.contains(uuid)) return "<yellow>Pending";
        long s = cooldownManager.getRemainingSeconds(uuid, COOLDOWN_ID);
        return s > 0 ? "<red>" + s + "s" : "<green>Ready";
    }

    @Override
    public StatusLevel statusLevel(UUID uuid) {
        if (pendingSet.contains(uuid)) return StatusLevel.PENDING;
        return cooldownManager.isOnCooldown(uuid, COOLDOWN_ID) ? StatusLevel.ON_COOLDOWN : StatusLevel.READY;
    }

    /**
     * Gives the player a second copy of the caught item if this catch is pending.
     * The clone is delivered two ticks later so the original item lands in inventory first.
     *
     * @param player  the catching player
     * @param uuid    the player's UUID
     * @param caught  the item that was caught
     * @param trigger the catch trigger to record the contribution on
     */
    public void applyOnCatch(Player player, UUID uuid, ItemStack caught, PassiveTrigger trigger) {
        if (!pendingSet.remove(uuid)) return;
        ItemStack clone = caught.clone();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.getInventory().addItem(clone);
                player.sendActionBar(MINI.deserialize(
                        "<aqua>Double Haul: <white>Second catch added to your inventory!"));
            }
        }, 2L);
        trigger.record(name(), "duplicate item queued", true);
    }
}
