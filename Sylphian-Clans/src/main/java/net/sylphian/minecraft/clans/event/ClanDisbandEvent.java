package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a clan is disbanded.
 *
 * <p>An invalidation signal carrying only the disbanded clan's UUID.</p>
 */
public class ClanDisbandEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;

    /**
     * @param clanId the UUID of the disbanded clan
     */
    public ClanDisbandEvent(UUID clanId) {
        this.clanId = clanId;
    }

    /** @return the UUID of the disbanded clan */
    public UUID getClanId() { return clanId; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
