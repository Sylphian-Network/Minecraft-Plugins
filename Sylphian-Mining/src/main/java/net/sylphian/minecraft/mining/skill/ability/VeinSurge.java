package net.sylphian.minecraft.mining.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.mining.config.MiningSkillConfig;
import net.sylphian.minecraft.mining.skill.trigger.OreHarvestTrigger;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActivationResult;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active unlocked at level 5.
 *
 * <p>Pending: the player's next mining harvest yields double.</p>
 */
public final class VeinSurge implements ActiveAbility {

    public static final String COOLDOWN_ID = "mining:vein-surge";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<MiningSkillConfig> config;
    private final CooldownManager cooldowns;
    private final Set<UUID> pending;

    /**
     * @param config    supplier for the current config snapshot
     * @param cooldowns the shared cooldown manager
     * @param pending   the set tracking whose next mine is doubled
     */
    public VeinSurge(Supplier<MiningSkillConfig> config, CooldownManager cooldowns, Set<UUID> pending) {
        this.config = config;
        this.cooldowns = cooldowns;
        this.pending = pending;
    }

    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Vein Surge"; }
    @Override public String description() { return "Your next mining harvest yields double."; }
    @Override public int    unlockLevel() { return 5; }

    @Override
    public ActivationResult onActivate(Player player, UUID uuid) {
        if (pending.contains(uuid)) {
            player.sendActionBar(MINI.deserialize("<yellow>Vein Surge <white>is already pending your next harvest."));
            return ActivationResult.blocked();
        }
        long remaining = cooldowns.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Vein Surge: <white>" + (remaining / 1000) + "s remaining."));
            return ActivationResult.blocked();
        }
        pending.add(uuid);
        cooldowns.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(config.get().veinSurgeCooldownSeconds()));
        player.sendActionBar(MINI.deserialize("<aqua>Vein Surge <white>ready! Your next harvest is doubled."));
        return ActivationResult.used("primed: next harvest doubled");
    }

    /**
     * Doubles the yield of the pending harvest, if any.
     *
     * @param uuid    the harvesting player's UUID
     * @param trigger the pre-harvest trigger to mutate and record on
     */
    public void applyOnHarvest(UUID uuid, OreHarvestTrigger trigger) {
        if (!pending.remove(uuid)) return;
        double multiplier = config.get().veinSurgeYieldMultiplier();
        trigger.event().setYieldMultiplier(trigger.event().getYieldMultiplier() * multiplier);
        trigger.record(name(), String.format("x%.1f yield", multiplier), true);
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
