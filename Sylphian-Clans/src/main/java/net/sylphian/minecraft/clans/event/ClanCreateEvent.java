package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a new clan is successfully created.
 *
 * <p>An invalidation signal: carries only the new clan's UUID. Listeners that
 * need full clan data should query via {@link net.sylphian.minecraft.clans.api.ClanProvider}.</p>
 */
public class ClanCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;

    /**
     * @param clanId the UUID of the newly created clan
     */
    public ClanCreateEvent(UUID clanId) {
        this.clanId = clanId;
    }

    /** @return the UUID of the newly created clan */
    public UUID getClanId() { return clanId; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
