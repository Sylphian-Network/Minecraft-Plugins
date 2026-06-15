package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a clan successfully claims a chunk.
 */
public class TerritoryClaimEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final String world;
    private final int chunkX;
    private final int chunkZ;

    /**
     * @param clanId the clan that claimed the chunk
     * @param world  the name of the world containing the chunk
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     */
    public TerritoryClaimEvent(UUID clanId, String world, int chunkX, int chunkZ) {
        this.clanId = clanId;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /** @return the clan that claimed the chunk */
    public UUID getClanId() { return clanId; }

    /** @return the name of the world containing the chunk */
    public String getWorld() { return world; }

    /** @return the chunk X coordinate */
    public int getChunkX() { return chunkX; }

    /** @return the chunk Z coordinate */
    public int getChunkZ() { return chunkZ; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
