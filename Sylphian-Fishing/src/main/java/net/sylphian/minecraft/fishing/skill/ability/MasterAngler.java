package net.sylphian.minecraft.fishing.skill.ability;

import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.fishing.skill.trigger.FishCastTrigger;
import net.sylphian.minecraft.fishing.skill.trigger.FishCatchTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive perk unlocked at level 30.
 *
 * <p>Permanently reduces hook wait times by a flat percentage and multiplies
 * all XP earned from fishing.</p>
 *
 * <p>Reacts to two triggers:</p>
 * <ul>
 *   <li>{@link FishCastTrigger} — contributes the timer reduction to the
 *       cast trigger's accumulator.</li>
 *   <li>{@link FishCatchTrigger} — contributes the XP multiplier to the
 *       catch trigger's accumulator.</li>
 * </ul>
 */
public final class MasterAngler implements PassiveAbility {

    private final Supplier<FishingSkillConfig> config;

    /**
     * @param config supplier for the current config snapshot
     */
    public MasterAngler(Supplier<FishingSkillConfig> config) {
        this.config = config;
    }

    @Override public String id()               { return "fishing:master-angler"; }
    @Override public String name()             { return "Master Angler"; }
    @Override public String description()      {
        return "Permanently reduces bite time by 20% and increases fishing XP by 50%.";
    }
    @Override public int    unlockLevel()      { return 30; }
    @Override public String triggerCondition() { return "On casting or catching a fish."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof FishCastTrigger || trigger instanceof FishCatchTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        FishingSkillConfig cfg = config.get();
        if (trigger instanceof FishCastTrigger castTrigger) {
            castTrigger.addReduction(cfg.masterAnglerReductionPercent() / 100.0);
        } else if (trigger instanceof FishCatchTrigger catchTrigger) {
            catchTrigger.multiplyXp(cfg.masterAnglerXpMultiplier());
        }
    }
}
