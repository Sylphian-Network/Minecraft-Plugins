package net.sylphian.minecraft.fishing.skill.ability;

import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.skills.skill.Ability;

/**
 * Passive perk unlocked at level 30.
 *
 * <p>Permanently reduces hook wait times by a flat percentage and multiplies
 * all XP earned from fishing.</p>
 */
public final class MasterAngler implements Ability {

    @Override public String id()           { return "fishing:master-angler"; }
    @Override public String name()         { return "Master Angler"; }
    @Override public String description()  {
        return "Permanently reduces bite time by 20% and increases fishing XP by 50%.";
    }
    @Override public int    unlockLevel()  { return 30; }

    /**
     * @param cfg current config snapshot
     * @return the fractional wait-time reduction (e.g. 0.20 for 20%)
     */
    public double reductionFraction(FishingSkillConfig cfg) {
        return cfg.masterAnglerReductionPercent() / 100.0;
    }

    /**
     * @param cfg current config snapshot
     * @return the XP multiplier (e.g. 1.5 for +50%)
     */
    public double xpMultiplier(FishingSkillConfig cfg) {
        return cfg.masterAnglerXpMultiplier();
    }
}
