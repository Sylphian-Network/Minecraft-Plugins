package net.sylphian.minecraft.cooking.skill;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable snapshot of Sylphian-Skills-specific cooking configuration.
 * Parse via {@link #from(FileConfiguration)}. Swap the reference on reload.
 *
 * @param xpPerRecipe base XP awarded per completed recipe, before quality and passive multipliers
 * @param discoveryXp one-time XP awarded the first time a player cooks a recipe
 * @param milestoneXp XP awarded each time a player reaches a mastery milestone for a recipe
 */
public record CookingSkillConfig(long xpPerRecipe, long discoveryXp, long milestoneXp) {

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
                config.getLong("cooking-skill.milestone-xp", 250L)
        );
    }
}
