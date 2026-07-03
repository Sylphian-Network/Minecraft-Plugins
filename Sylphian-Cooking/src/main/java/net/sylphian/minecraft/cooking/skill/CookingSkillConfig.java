package net.sylphian.minecraft.cooking.skill;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable snapshot of Sylphian-Skills-specific cooking configuration.
 * Parse via {@link #from(FileConfiguration)}. Swap the reference on reload.
 *
 * @param xpPerRecipe                   base XP per completed recipe, before quality and passive multipliers
 * @param discoveryXp                   one-time XP awarded the first time a player cooks a recipe
 * @param milestoneXp                   XP awarded each time a player reaches a mastery milestone
 * @param efficientCookChancePercent    Efficient Cook chance to spare one ingredient per cook
 * @param quickPrepReductionPercent     Quick Prep flat cook-time reduction
 * @param seasonedHandsMaxStacks        Seasoned Hands maximum streak stacks
 * @param seasonedHandsQualityPerStack  Perfect-weight added per Seasoned Hands stack
 * @param seasonedHandsXpPerStackPercent XP percent added per Seasoned Hands stack
 * @param seasonedHandsResetSeconds     seconds without a cook before the streak resets
 * @param seasonedHandsResetDistance    blocks a player may move between cooks before the streak resets
 * @param banquetRadius                 Banquet effect radius in blocks
 * @param banquetDurationSeconds        Banquet buff duration in seconds
 * @param banquetAmplifier              Banquet potion effect amplifier (0 = level I)
 * @param banquetCooldownSeconds        Banquet cooldown in seconds
 * @param secondWindCooldownSeconds     Second Wind cooldown in seconds
 * @param perfectSearCooldownSeconds    Perfect Sear cooldown in seconds
 */
public record CookingSkillConfig(
        long   xpPerRecipe,
        long   discoveryXp,
        long   milestoneXp,
        double efficientCookChancePercent,
        double quickPrepReductionPercent,
        int    seasonedHandsMaxStacks,
        double seasonedHandsQualityPerStack,
        double seasonedHandsXpPerStackPercent,
        long   seasonedHandsResetSeconds,
        double seasonedHandsResetDistance,
        double banquetRadius,
        int    banquetDurationSeconds,
        int    banquetAmplifier,
        long   banquetCooldownSeconds,
        long   secondWindCooldownSeconds,
        long   perfectSearCooldownSeconds
) {

    /**
     * Parses a {@link CookingSkillConfig} from the plugin's {@code config.yml}.
     *
     * @param config the loaded file configuration
     * @return the parsed snapshot
     */
    public static CookingSkillConfig from(FileConfiguration config) {
        return new CookingSkillConfig(
                config.getLong("cooking-skill.xp-per-recipe", 10L),
                config.getLong("cooking-skill.discovery-xp", 100L),
                config.getLong("cooking-skill.milestone-xp", 250L),
                config.getDouble("cooking-skill.efficient-cook.chance-percent", 15.0),
                config.getDouble("cooking-skill.quick-prep.reduction-percent", 20.0),
                config.getInt("cooking-skill.seasoned-hands.max-stacks", 5),
                config.getDouble("cooking-skill.seasoned-hands.quality-per-stack", 2.0),
                config.getDouble("cooking-skill.seasoned-hands.xp-per-stack-percent", 5.0),
                config.getLong("cooking-skill.seasoned-hands.reset-seconds", 30L),
                config.getDouble("cooking-skill.seasoned-hands.reset-distance", 10.0),
                config.getDouble("cooking-skill.banquet.radius", 8.0),
                config.getInt("cooking-skill.banquet.duration-seconds", 30),
                config.getInt("cooking-skill.banquet.amplifier", 0),
                config.getLong("cooking-skill.banquet.cooldown-seconds", 600L),
                config.getLong("cooking-skill.second-wind.cooldown-seconds", 60L),
                config.getLong("cooking-skill.perfect-sear.cooldown-seconds", 300L)
        );
    }
}
