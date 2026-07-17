package net.sylphian.minecraft.logging.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.logging.config.LoggingSkillConfig;
import net.sylphian.minecraft.logging.skill.trigger.LogHarvestTrigger;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active unlocked at level 28.
 *
 * <p>Pending: the player's next logging harvest yields double and leaves the
 * node standing (it does not deplete).</p>
 */
public final class AncientTimber implements ActiveAbility {

    public static final String COOLDOWN_ID = "logging:ancient-timber";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<LoggingSkillConfig> config;
    private final CooldownManager cooldowns;
    private final Set<UUID> pending;

    /**
     * @param config    supplier for the current config snapshot
     * @param cooldowns the shared cooldown manager
     * @param pending   the set tracking whose next chop is doubled and non-depleting
     */
    public AncientTimber(Supplier<LoggingSkillConfig> config, CooldownManager cooldowns, Set<UUID> pending) {
        this.config = config;
        this.cooldowns = cooldowns;
        this.pending = pending;
    }

    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Ancient Timber"; }
    @Override public String description() { return "Your next harvest yields double and leaves the tree standing."; }
    @Override public int    unlockLevel() { return 28; }

    @Override
    public void onActivate(Player player, UUID uuid) {
        if (pending.contains(uuid)) {
            player.sendActionBar(MINI.deserialize("<yellow>Ancient Timber <white>is already pending your next harvest."));
            return;
        }
        long remaining = cooldowns.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Ancient Timber: <white>" + (remaining / 1000) + "s remaining."));
            return;
        }
        pending.add(uuid);
        cooldowns.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(config.get().ancientTimberCooldownSeconds()));
        player.sendActionBar(MINI.deserialize("<aqua>Ancient Timber <white>ready! Your next harvest spares the tree."));
    }

    /**
     * Doubles the pending harvest's yield and suppresses depletion, if pending.
     *
     * @param uuid    the harvesting player's UUID
     * @param trigger the pre-harvest trigger to mutate and record on
     */
    public void applyOnHarvest(UUID uuid, LogHarvestTrigger trigger) {
        if (!pending.remove(uuid)) return;
        NodeHarvestEvent event = trigger.event();
        double multiplier = config.get().ancientTimberYieldMultiplier();
        event.setYieldMultiplier(event.getYieldMultiplier() * multiplier);
        event.setDeplete(false);
        trigger.record(name(), String.format("x%.1f yield, node kept", multiplier), true);
    }

    @Override
    public String selectionStatus(UUID uuid) {
        if (pending.contains(uuid)) return "<yellow>Pending";
        long s = cooldowns.getRemainingSeconds(uuid, COOLDOWN_ID);
        return s > 0 ? "<red>" + s + "s" : "<green>Ready";
    }

    @Override
    public StatusLevel statusLevel(UUID uuid) {
        if (pending.contains(uuid)) return StatusLevel.PENDING;
        return cooldowns.isOnCooldown(uuid, COOLDOWN_ID) ? StatusLevel.ON_COOLDOWN : StatusLevel.READY;
    }
}
