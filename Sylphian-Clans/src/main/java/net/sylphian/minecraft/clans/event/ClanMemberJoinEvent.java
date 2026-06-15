package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a player successfully joins a clan.
 *
 * <p>An invalidation signal: carries the clan UUID and the joining player's UUID.
 * Listeners that need updated membership data should re-query via
 * {@link net.sylphian.minecraft.clans.api.ClanProvider}.</p>
 */
public class ClanMemberJoinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final UUID playerUuid;

    /**
     * @param clanId     the UUID of the clan that was joined
     * @param playerUuid the UUID of the player who joined
     */
    public ClanMemberJoinEvent(UUID clanId, UUID playerUuid) {
        this.clanId = clanId;
        this.playerUuid = playerUuid;
    }

    /** @return the UUID of the clan that was joined */
    public UUID getClanId() { return clanId; }

    /** @return the UUID of the player who joined */
    public UUID getPlayerUuid() { return playerUuid; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
