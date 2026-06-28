package net.sylphian.minecraft.skills.api;

import net.sylphian.minecraft.skills.service.ActiveBuffTracker;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.Skill;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public cross-plugin contract for Sylphian-Skills, obtained via {@link SkillsProvider}.
 */
public interface SkillsAPI {

    /**
     * Registers a skill contributed by an external plugin and immediately activates
     * its listeners. The owning plugin is used when registering Bukkit event handlers
     * so they are tied to that plugin's lifecycle.
     *
     * <p>Call from the owning plugin's {@code onEnable}. The skill ID must be unique
     * across all registered skills.</p>
     *
     * @param skill        the skill to register
     * @param owningPlugin the plugin that owns this skill
     * @throws IllegalArgumentException if a skill with the same ID is already registered
     */
    void registerSkill(Skill skill, Plugin owningPlugin);

    /**
     * Removes a skill from the registry. Bukkit automatically unregisters the skill's
     * event listeners when the owning plugin unloads, so no explicit listener teardown
     * is needed here.
     *
     * <p>Call from the owning plugin's {@code onDisable}.</p>
     *
     * @param skillId the skill identifier to remove
     */
    void unregisterSkill(String skillId);

    /**
     * @return the shared cooldown manager for tracking active skill cooldowns
     */
    CooldownManager getCooldownManager();

    /**
     * @return the shared buff tracker for tracking timed active skill buffs
     */
    ActiveBuffTracker getActiveBuffTracker();

    /**
     * Returns a player's cached XP for a skill, suitable for synchronous reads
     * such as sidebar rendering. Returns 0 if the player is not cached.
     *
     * @param uuid    the player's UUID
     * @param skillId the skill identifier
     * @return cached XP, or 0 if not loaded
     */
    long getCachedXP(UUID uuid, String skillId);

    /**
     * Returns a player's current level for a skill derived from their cached XP.
     * Returns 0 if the player is not cached.
     *
     * @param uuid    the player's UUID
     * @param skillId the skill identifier
     * @return current level, or 0 if not loaded
     */
    int getCachedLevel(UUID uuid, String skillId);

    /**
     * Awards XP to a player in the given skill. Fires {@link net.sylphian.minecraft.skills.event.SkillXPGainEvent}
     * and, if a level boundary is crossed, {@link net.sylphian.minecraft.skills.event.SkillLevelUpEvent}.
     * Must be called from the main thread.
     *
     * @param player  the player receiving XP
     * @param skillId the skill identifier
     * @param amount  a positive amount of XP to award
     */
    void awardXP(Player player, String skillId, long amount);

    /**
     * Reads a player's XP directly from the database.
     *
     * @param uuid    the player's UUID
     * @param skillId the skill identifier
     * @return a future of the stored XP value
     */
    CompletableFuture<Long> getXP(UUID uuid, String skillId);

    /**
     * Returns whether a player is at the level cap for a skill.
     * Uses the cached XP, so returns {@code false} if the player is not loaded.
     *
     * @param uuid    the player's UUID
     * @param skillId the skill identifier
     * @return {@code true} if the player's cached XP equals or exceeds the cap
     */
    boolean isAtCap(UUID uuid, String skillId);

    /**
     * @return all registered skills in insertion order
     */
    Collection<Skill> getSkills();

    /**
     * Looks up a registered skill by its identifier.
     *
     * @param skillId the skill identifier
     * @return the skill, or empty if not registered
     */
    Optional<Skill> getSkill(String skillId);
}
