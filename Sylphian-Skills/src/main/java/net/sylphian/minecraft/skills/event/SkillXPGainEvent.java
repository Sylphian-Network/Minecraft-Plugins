package net.sylphian.minecraft.skills.event;

import net.sylphian.minecraft.skills.skill.Skill;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired on the main thread after a player gains XP in a skill.
 *
 * <p>Carries the UUID rather than a {@code Player} so the event can be
 * re-fired by a future cross-server messenger without needing an online player.</p>
 */
public class SkillXPGainEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final Skill skill;
    private final long amount;
    private final long newTotalXP;

    /**
     * @param playerId   the player who gained XP
     * @param skill      the skill that was levelled
     * @param amount     the amount of XP awarded
     * @param newTotalXP the player's new total XP in this skill after the gain
     */
    public SkillXPGainEvent(UUID playerId, Skill skill, long amount, long newTotalXP) {
        this.playerId = playerId;
        this.skill = skill;
        this.amount = amount;
        this.newTotalXP = newTotalXP;
    }

    /** @return the player who gained XP */
    public UUID getPlayerId() { return playerId; }

    /** @return the skill that awarded XP */
    public Skill getSkill() { return skill; }

    /** @return the amount of XP gained in this event */
    public long getAmount() { return amount; }

    /** @return the player's new total XP in this skill */
    public long getNewTotalXP() { return newTotalXP; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
