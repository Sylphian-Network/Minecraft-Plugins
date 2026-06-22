package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a clan creates a new warp. Does not fire when an existing
 * warp is merely updated.
 */
public class ClanWarpCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final String warpName;
    private final String world;

    /**
     * @param clanId   the clan that owns the warp
     * @param warpName the warp name
     * @param world    the world the warp is located in
     */
    public ClanWarpCreateEvent(UUID clanId, String warpName, String world) {
        this.clanId = clanId;
        this.warpName = warpName;
        this.world = world;
    }

    /** @return the clan that owns the warp */
    public UUID getClanId() { return clanId; }

    /** @return the warp name */
    public String getWarpName() { return warpName; }

    /** @return the world the warp is located in */
    public String getWorld() { return world; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
