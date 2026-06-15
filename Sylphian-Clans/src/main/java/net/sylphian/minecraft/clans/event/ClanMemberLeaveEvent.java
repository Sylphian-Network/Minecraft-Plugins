package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a player leaves or is kicked from a clan.
 *
 * <p>An invalidation signal: carries the clan UUID and the departing player's UUID.
 * Does not fire when a clan is disbanded, {@link ClanDisbandEvent} covers that case.</p>
 */
public class ClanMemberLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final UUID playerUuid;

    /**
     * @param clanId     the UUID of the clan that was left
     * @param playerUuid the UUID of the player who left or was kicked
     */
    public ClanMemberLeaveEvent(UUID clanId, UUID playerUuid) {
        this.clanId = clanId;
        this.playerUuid = playerUuid;
    }

    /** @return the UUID of the clan that was left */
    public UUID getClanId() { return clanId; }

    /** @return the UUID of the player who left or was kicked */
    public UUID getPlayerUuid() { return playerUuid; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
