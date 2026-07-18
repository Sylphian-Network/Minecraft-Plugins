package net.sylphian.minecraft.logging.skill.ability;

import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.logging.config.LoggingSkillConfig;
import net.sylphian.minecraft.logging.skill.trigger.LogHarvestTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Passive unlocked at level 10.
 *
 * <p>Rolls a chance, scaling with level, for the whole trunk to fall at once,
 * multiplying the harvest yield.</p>
 */
public final class TimberFall implements PassiveAbility {

    private final Supplier<LoggingSkillConfig> config;
    private final ToIntFunction<UUID> levelOf;
    private final Random random = new Random();

    /**
     * @param config  supplier for the current config snapshot
     * @param levelOf lookup for a player's current logging level
     */
    public TimberFall(Supplier<LoggingSkillConfig> config, ToIntFunction<UUID> levelOf) {
        this.config = config;
        this.levelOf = levelOf;
    }

    @Override public String id()               { return "logging:timber-fall"; }
    @Override public String name()             { return "Timber Fall"; }
    @Override public String description()      { return "A chance, growing with level, to fell the whole trunk for triple yield."; }
    @Override public int    unlockLevel()      { return 10; }
    @Override public String triggerCondition() { return "On harvesting a logging node."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof LogHarvestTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        NodeHarvestEvent event = ((LogHarvestTrigger) trigger).event();
        LoggingSkillConfig cfg = config.get();

        double chance = cfg.timberFallBaseChance() + cfg.timberFallChancePerLevel() * levelOf.applyAsInt(uuid);
        if (random.nextDouble() >= chance) return;

        event.setYieldMultiplier(event.getYieldMultiplier() * cfg.timberFallYieldMultiplier());
        trigger.record(name(), String.format("x%.1f yield (whole trunk)", cfg.timberFallYieldMultiplier()));
    }
}
