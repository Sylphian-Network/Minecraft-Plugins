package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread when a player walks into a chunk owned by a clan, having just
 * been in wilderness or another clan's territory. Not cancellable, movement is not blocked.
 */
public class ClanTerritoryEnterEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final UUID clanId;
    private final String world;
    private final int chunkX;
    private final int chunkZ;

    /**
     * @param playerUuid the player who entered the territory
     * @param clanId     the clan that owns the entered chunk
     * @param world      the world containing the chunk
     * @param chunkX     the chunk X coordinate
     * @param chunkZ     the chunk Z coordinate
     */
    public ClanTerritoryEnterEvent(UUID playerUuid, UUID clanId, String world, int chunkX, int chunkZ) {
        this.playerUuid = playerUuid;
        this.clanId = clanId;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /** @return the player who entered the territory */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return the clan that owns the entered chunk */
    public UUID getClanId() { return clanId; }

    /** @return the world containing the chunk */
    public String getWorld() { return world; }

    /** @return the chunk X coordinate */
    public int getChunkX() { return chunkX; }

    /** @return the chunk Z coordinate */
    public int getChunkZ() { return chunkZ; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
