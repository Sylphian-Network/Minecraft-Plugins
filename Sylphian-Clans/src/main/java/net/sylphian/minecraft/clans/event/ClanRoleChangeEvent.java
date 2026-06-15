package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a clan member's role changes.
 *
 * <p>An invalidation signal: carries only the clan UUID and the affected player's UUID.
 * Listeners that need the new role should re-query via
 * {@link net.sylphian.minecraft.clans.api.ClanProvider}.</p>
 */
public class ClanRoleChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final UUID playerUuid;

    /**
     * @param clanId     the UUID of the clan in which the role changed
     * @param playerUuid the UUID of the player whose role changed
     */
    public ClanRoleChangeEvent(UUID clanId, UUID playerUuid) {
        this.clanId = clanId;
        this.playerUuid = playerUuid;
    }

    /** @return the UUID of the clan in which the role changed */
    public UUID getClanId() { return clanId; }

    /** @return the UUID of the player whose role changed */
    public UUID getPlayerUuid() { return playerUuid; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
