package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a player leaves or is kicked from a clan. The
 * {@link #getCause() cause} distinguishes a voluntary leave from a kick. Does not fire on
 * disbandment, {@link ClanDisbandEvent} covers that.
 */
public class ClanMemberLeaveEvent extends Event {

    /** Why a member was removed. */
    public enum Cause {
        /** The member chose to leave. */
        LEAVE,
        /** The member was kicked by another player or an administrator. */
        KICK
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final UUID playerUuid;
    private final Cause cause;

    /**
     * @param clanId     the UUID of the clan being left
     * @param playerUuid the UUID of the departing player
     * @param cause      whether the player left voluntarily or was kicked
     */
    public ClanMemberLeaveEvent(UUID clanId, UUID playerUuid, Cause cause) {
        this.clanId = clanId;
        this.playerUuid = playerUuid;
        this.cause = cause;
    }

    /** @return the UUID of the clan being left */
    public UUID getClanId() { return clanId; }

    /** @return the UUID of the departing player */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return whether the player left voluntarily or was kicked */
    public Cause getCause() { return cause; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
