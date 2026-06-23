package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a clan warp is removed.
 */
public class ClanWarpDeleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final String warpName;

    /**
     * @param clanId   the clan that owned the warp
     * @param warpName the name of the removed warp
     */
    public ClanWarpDeleteEvent(UUID clanId, String warpName) {
        this.clanId = clanId;
        this.warpName = warpName;
    }

    /** @return the clan that owned the warp */
    public UUID getClanId() { return clanId; }

    /** @return the name of the removed warp */
    public String getWarpName() { return warpName; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
