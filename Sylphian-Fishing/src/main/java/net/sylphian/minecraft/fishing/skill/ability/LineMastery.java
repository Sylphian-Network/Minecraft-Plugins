package net.sylphian.minecraft.fishing.skill.ability;

import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.skills.skill.Ability;

/**
 * Passive perk unlocked at level 10.
 *
 * <p>Permanently reduces hook wait times by a flat percentage on every cast.</p>
 */
public final class LineMastery implements Ability {

    @Override public String id()           { return "fishing:line-mastery"; }
    @Override public String name()         { return "Line Mastery"; }
    @Override public String description()  { return "Passively reduces bite wait time by 15%."; }
    @Override public int    unlockLevel()  { return 10; }

    /**
     * @param cfg current config snapshot
     * @return the fractional wait-time reduction (e.g. 0.15 for 15%)
     */
    public double reductionFraction(FishingSkillConfig cfg) {
        return cfg.lineMasteryReductionPercent() / 100.0;
    }
}
