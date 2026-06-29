package net.sylphian.minecraft.cooking.skill;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable snapshot of all cooking-skill configuration values.
 *
 * <p>Parse via {@link #from(FileConfiguration)}. Swap the reference on reload;
 * the listener reads through the volatile field in {@link CookingSkill}.</p>
 *
 * @param xpPerRecipe                    base XP awarded per completed recipe
 * @param quickCureReductionPercent      permanent cook-time reduction from Quick Cure
 * @param bonusYieldChancePercent        chance (0–100) for Bonus Yield to drop an extra output
 * @param flashFireCooldownSeconds       cooldown for Flash Fire
 * @param masterChefReductionPercent     additional cook-time reduction from Master Chef
 * @param masterChefXpMultiplier         XP multiplier applied by Master Chef
 */
public record CookingSkillConfig(
        long   xpPerRecipe,
        double quickCureReductionPercent,
        double bonusYieldChancePercent,
        int    flashFireCooldownSeconds,
        double masterChefReductionPercent,
        double masterChefXpMultiplier
) {

    /**
     * Parses a {@link CookingSkillConfig} from the cooking plugin's {@code config.yml}.
     * All keys have safe defaults so a missing section never aborts startup.
     *
     * @param config the loaded file configuration
     * @return the parsed snapshot
     */
    public static CookingSkillConfig from(FileConfiguration config) {
        return new CookingSkillConfig(
                config.getLong  ("cooking-skill.xp-per-recipe",                       10L),
                config.getDouble("cooking-skill.quick-cure.reduction-percent",        15.0),
                config.getDouble("cooking-skill.bonus-yield.chance-percent",          20.0),
                config.getInt   ("cooking-skill.flash-fire.cooldown-seconds",         120),
                config.getDouble("cooking-skill.master-chef.reduction-percent",       20.0),
                config.getDouble("cooking-skill.master-chef.xp-multiplier",           1.5)
        );
    }
}
