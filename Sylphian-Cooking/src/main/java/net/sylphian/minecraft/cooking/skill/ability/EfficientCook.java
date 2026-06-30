package net.sylphian.minecraft.cooking.skill.ability;

import net.sylphian.minecraft.cooking.skill.CookingSkillConfig;
import net.sylphian.minecraft.cooking.skill.trigger.CookingCompleteTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive perk unlocked at level 10. Each cook has a chance to spare one ingredient from being consumed.
 */
public final class EfficientCook implements PassiveAbility {

    private final Supplier<CookingSkillConfig> config;

    /**
     * @param config supplier for the current config snapshot
     */
    public EfficientCook(Supplier<CookingSkillConfig> config) {
        this.config = config;
    }

    @Override public String id()               { return "cooking:efficient-cook"; }
    @Override public String name()             { return "Efficient Cook"; }
    @Override public String description()      { return "Chance to not consume one ingredient per cook."; }
    @Override public int    unlockLevel()      { return 10; }
    @Override public String triggerCondition() { return "On completing a cook."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof CookingCompleteTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        double pct = config.get().efficientCookChancePercent();
        if (ThreadLocalRandom.current().nextDouble(100.0) < pct) {
            ((CookingCompleteTrigger) trigger).preserveIngredient();
            trigger.record(name(), "ingredient spared", false);
        }
    }
}
