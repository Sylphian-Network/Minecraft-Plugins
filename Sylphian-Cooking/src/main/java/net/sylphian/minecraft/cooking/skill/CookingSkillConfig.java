package net.sylphian.minecraft.cooking.skill;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable snapshot of Sylphian-Skills-specific cooking configuration.
 * Parse via {@link #from(FileConfiguration)}. Swap the reference on reload.
 *
 * @param xpPerRecipe base XP awarded per completed recipe, before quality and passive multipliers
 */
public record CookingSkillConfig(long xpPerRecipe) {

    /**
     * Parses a {@link CookingSkillConfig} from the plugin's {@code config.yml}.
     *
     * @param config the loaded file configuration
     * @return the parsed snapshot
     */
    public static CookingSkillConfig from(FileConfiguration config) {
        return new CookingSkillConfig(config.getLong("cooking-skill.xp-per-recipe", 10L));
    }
}
