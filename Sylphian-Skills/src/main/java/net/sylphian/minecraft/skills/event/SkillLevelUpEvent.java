package net.sylphian.minecraft.skills.event;

import net.sylphian.minecraft.skills.skill.Skill;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired on the main thread when a player's level in a skill increases.
 *
 * <p>A single XP gain can span multiple levels (e.g. 1 → 3); in that case
 * this event fires once with {@code oldLevel=1} and {@code newLevel=3}.</p>
 */
public class SkillLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final Skill skill;
    private final int oldLevel;
    private final int newLevel;

    /**
     * @param playerId the player who levelled up
     * @param skill    the skill that increased
     * @param oldLevel the level before the gain
     * @param newLevel the level after the gain
     */
    public SkillLevelUpEvent(UUID playerId, Skill skill, int oldLevel, int newLevel) {
        this.playerId = playerId;
        this.skill = skill;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    /** @return the player who levelled up */
    public UUID getPlayerId() { return playerId; }

    /** @return the skill that increased */
    public Skill getSkill() { return skill; }

    /** @return the level before this gain */
    public int getOldLevel() { return oldLevel; }

    /** @return the level after this gain */
    public int getNewLevel() { return newLevel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
