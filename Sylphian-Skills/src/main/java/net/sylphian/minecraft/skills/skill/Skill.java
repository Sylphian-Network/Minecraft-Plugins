package net.sylphian.minecraft.skills.skill;

import net.sylphian.minecraft.skills.api.SkillsAPI;
import org.bukkit.plugin.Plugin;

/**
 * Contract for a single skill implementation.
 *
 * <p>Each skill is responsible for identifying itself and registering the
 * Bukkit event listeners that award XP by calling back into
 * {@link SkillsAPI#awardXP}.</p>
 */
public interface Skill {

    /**
     * @return the unique lowercase identifier for this skill, e.g. {@code "mining"}
     */
    String getId();

    /**
     * @return the player-facing display name, e.g. {@code "Mining"}
     */
    String getDisplayName();

    /**
     * Registers this skill's Bukkit event listeners with the server.
     * Called once during plugin enable after the service is ready.
     *
     * @param plugin the owning plugin (used as listener owner)
     * @param api    the skills API for XP awards, cooldowns, and buff tracking
     */
    void registerListeners(Plugin plugin, SkillsAPI api);

    /**
     * Called when the owning plugin reloads its config. No-op by default.
     */
    default void reload() {}
}
