package net.sylphian.minecraft.logging.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.node.LootEntry;
import net.sylphian.minecraft.gathering.node.LootTable;
import net.sylphian.minecraft.logging.config.LoggingSkillConfig;
import net.sylphian.minecraft.logging.skill.trigger.LogHarvestTrigger;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActivationResult;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active unlocked at level 5.
 *
 * <p>Pending: the player's next logging harvest additionally drops the rarest
 * entry in the node's loot table (the lowest-weight row, ignoring weighting).</p>
 */
public final class HeartwoodStrike implements ActiveAbility {

    public static final String COOLDOWN_ID = "logging:heartwood-strike";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<LoggingSkillConfig> config;
    private final CooldownManager cooldowns;
    private final Set<UUID> pending;

    /**
     * @param config    supplier for the current config snapshot
     * @param cooldowns the shared cooldown manager
     * @param pending   the set tracking whose next chop drops the rarest entry
     */
    public HeartwoodStrike(Supplier<LoggingSkillConfig> config, CooldownManager cooldowns, Set<UUID> pending) {
        this.config = config;
        this.cooldowns = cooldowns;
        this.pending = pending;
    }

    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Heartwood Strike"; }
    @Override public String description() { return "Your next harvest also drops the rarest item the tree can yield."; }
    @Override public int    unlockLevel() { return 5; }

    @Override
    public ActivationResult onActivate(Player player, UUID uuid) {
        if (pending.contains(uuid)) {
            player.sendActionBar(MINI.deserialize("<yellow>Heartwood Strike <white>is already pending your next harvest."));
            return ActivationResult.blocked();
        }
        long remaining = cooldowns.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Heartwood Strike: <white>" + (remaining / 1000) + "s remaining."));
            return ActivationResult.blocked();
        }
        pending.add(uuid);
        cooldowns.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(config.get().heartwoodStrikeCooldownSeconds()));
        player.sendActionBar(MINI.deserialize("<aqua>Heartwood Strike <white>ready! Your next harvest bites deep."));
        return ActivationResult.used("primed: next harvest drops rarest entry");
    }

    /**
     * Adds the node's rarest loot entry to the harvest, if this harvest is pending.
     *
     * @param uuid    the harvesting player's UUID
     * @param trigger the pre-harvest trigger to mutate and record on
     */
    public void applyOnHarvest(UUID uuid, LogHarvestTrigger trigger) {
        if (!pending.remove(uuid)) return;

        LootTable loot = trigger.event().node().type().loot();
        LootEntry rarest = loot.entries().stream()
                .min(Comparator.comparingInt(LootEntry::weight))
                .orElse(null);
        if (rarest == null) return;

        trigger.event().addBonusLoot(rarest);
        trigger.record(name(), "+" + rarest.itemId() + " (rarest entry)", true);
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
