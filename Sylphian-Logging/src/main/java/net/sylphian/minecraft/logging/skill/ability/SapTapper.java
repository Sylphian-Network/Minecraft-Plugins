package net.sylphian.minecraft.logging.skill.ability;

import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.gathering.node.LootEntry;
import net.sylphian.minecraft.logging.config.LoggingSkillConfig;
import net.sylphian.minecraft.logging.skill.trigger.LogHarvestTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive unlocked at level 12.
 *
 * <p>Rolls a flat chance to also drop tree sap on a logging harvest.</p>
 */
public final class SapTapper implements PassiveAbility {

    private final Supplier<LoggingSkillConfig> config;
    private final Random random = new Random();

    /**
     * @param config supplier for the current config snapshot
     */
    public SapTapper(Supplier<LoggingSkillConfig> config) {
        this.config = config;
    }

    @Override public String id()               { return "logging:sap-tapper"; }
    @Override public String name()             { return "Sap Tapper"; }
    @Override public String description()      { return "A chance to also tap tree sap from the trunk."; }
    @Override public int    unlockLevel()      { return 12; }
    @Override public String triggerCondition() { return "On harvesting a logging node."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof LogHarvestTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        NodeHarvestEvent event = ((LogHarvestTrigger) trigger).event();
        LoggingSkillConfig cfg = config.get();

        if (random.nextDouble() >= cfg.sapTapperChance()) return;
        event.addBonusLoot(new LootEntry(cfg.sapTapperItem(), 1, 1, 1));
        trigger.record(name(), "+1 " + cfg.sapTapperItem());
    }
}
