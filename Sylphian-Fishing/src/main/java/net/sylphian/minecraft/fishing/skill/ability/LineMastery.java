package net.sylphian.minecraft.fishing.skill.ability;

import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.fishing.skill.trigger.FishCastTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive perk unlocked at level 10.
 *
 * <p>Permanently reduces hook wait times by a flat percentage on every cast.</p>
 */
public final class LineMastery implements PassiveAbility {

    private final Supplier<FishingSkillConfig> config;

    /**
     * @param config supplier for the current config snapshot
     */
    public LineMastery(Supplier<FishingSkillConfig> config) {
        this.config = config;
    }

    @Override public String id()               { return "fishing:line-mastery"; }
    @Override public String name()             { return "Line Mastery"; }
    @Override public String description()      { return "Passively reduces bite wait time by 15%."; }
    @Override public int    unlockLevel()      { return 10; }
    @Override public String triggerCondition() { return "On casting a fishing rod."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof FishCastTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        double pct = config.get().lineMasteryReductionPercent();
        ((FishCastTrigger) trigger).addReduction(pct / 100.0);
        trigger.record(name(), "-" + (int) pct + "% cast timer");
    }
}
