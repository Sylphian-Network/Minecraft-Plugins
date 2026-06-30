package net.sylphian.minecraft.cooking.skill.ability;

import net.sylphian.minecraft.cooking.skill.CookingSkillConfig;
import net.sylphian.minecraft.cooking.skill.trigger.CookingStartTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive perk unlocked at level 30. Passively reduces cook time by a flat percentage.
 */
public final class QuickPrep implements PassiveAbility {

    private final Supplier<CookingSkillConfig> config;

    /**
     * @param config supplier for the current config snapshot
     */
    public QuickPrep(Supplier<CookingSkillConfig> config) {
        this.config = config;
    }

    @Override public String id()               { return "cooking:quick-prep"; }
    @Override public String name()             { return "Quick Prep"; }
    @Override public String description()      { return "Passively reduces cook time."; }
    @Override public int    unlockLevel()      { return 30; }
    @Override public String triggerCondition() { return "On starting a cook."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof CookingStartTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        double pct = config.get().quickPrepReductionPercent();
        ((CookingStartTrigger) trigger).addReduction(pct / 100.0);
        trigger.record(name(), "-" + (int) pct + "% cook time", false);
    }
}
