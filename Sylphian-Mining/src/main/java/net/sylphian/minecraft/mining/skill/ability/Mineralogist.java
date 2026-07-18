package net.sylphian.minecraft.mining.skill.ability;

import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.gathering.node.NodeModifier;
import net.sylphian.minecraft.mining.config.MiningSkillConfig;
import net.sylphian.minecraft.mining.skill.trigger.OreHarvestTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive unlocked at level 20.
 *
 * <p>Grants bonus XP when the harvested node rolled an active modifier (an
 * enriched vein), scaling with the modifier's yield multiplier. A plain,
 * unmodified node gives no bonus.</p>
 */
public final class Mineralogist implements PassiveAbility {

    private final Supplier<MiningSkillConfig> config;

    /**
     * @param config supplier for the current config snapshot
     */
    public Mineralogist(Supplier<MiningSkillConfig> config) {
        this.config = config;
    }

    @Override public String id()               { return "mining:mineralogist"; }
    @Override public String name()             { return "Mineralogist"; }
    @Override public String description()      { return "Enriched veins grant bonus XP, more for the richest ones."; }
    @Override public int    unlockLevel()      { return 20; }
    @Override public String triggerCondition() { return "On harvesting an enriched mining node."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof OreHarvestTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        NodeHarvestEvent event = ((OreHarvestTrigger) trigger).event();
        NodeModifier modifier = event.node().activeModifier();
        if (modifier == null) return;

        MiningSkillConfig cfg = config.get();
        double multiplier = 1.0 + cfg.mineralogistBaseBonus()
                + cfg.mineralogistPerYieldBonus() * Math.max(0.0, modifier.yieldMultiplier() - 1.0);
        if (multiplier <= 1.0) return;

        event.setXpMultiplier(event.getXpMultiplier() * multiplier);
        trigger.record(name(), String.format("x%.2f XP (%s vein)", multiplier, modifier.id()));
    }
}
