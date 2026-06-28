package net.sylphian.minecraft.skills.event;

import net.sylphian.minecraft.skills.skill.Skill;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired on the main thread the first time a player reaches the level cap in a skill.
 *
 * <p>This event fires exactly once per cap transition: when XP crosses from below
 * {@code xpForLevel(levelCap())} to the cap. It does not fire again if the player
 * is already at cap and gains more XP (that path is a no-op in {@code awardXP}).</p>
 *
 * <p>Other modules can listen to this event to reward the player, make an
 * announcement, or unlock new content.</p>
 */
public class SkillMaxLevelEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final Skill skill;
    private final int level;

    /**
     * @param playerId the player who reached the cap
     * @param skill    the skill that reached the cap
     * @param level    the level cap value that was reached
     */
    public SkillMaxLevelEvent(UUID playerId, Skill skill, int level) {
        this.playerId = playerId;
        this.skill    = skill;
        this.level    = level;
    }

    /** @return the player who reached the cap */
    public UUID getPlayerId() { return playerId; }

    /** @return the skill that reached the cap */
    public Skill getSkill() { return skill; }

    /** @return the level cap value that was reached */
    public int getLevel() { return level; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
