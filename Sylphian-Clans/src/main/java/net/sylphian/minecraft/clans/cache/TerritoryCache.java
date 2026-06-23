package net.sylphian.minecraft.clans.cache;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache of chunk ownership, keyed on {@code world:chunkX:chunkZ}.
 *
 * <p>Supports synchronous, non-blocking reads on the main thread. The database
 * is the source of truth.</p>
 */
public class TerritoryCache {

    private final Map<String, UUID> chunkToClan = new ConcurrentHashMap<>();

    /**
     * Records a claim in the cache.
     *
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @param clanId the owning clan's UUID
     */
    public void put(String world, int chunkX, int chunkZ, UUID clanId) {
        chunkToClan.put(key(world, chunkX, chunkZ), clanId);
    }

    /**
     * Removes a specific chunk from the cache.
     *
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     */
    public void remove(String world, int chunkX, int chunkZ) {
        chunkToClan.remove(key(world, chunkX, chunkZ));
    }

    /**
     * Removes all chunks owned by the given clan. Used when a clan is disbanded.
     *
     * @param clanId the clan whose claims to evict
     */
    public void removeAllForClan(UUID clanId) {
        chunkToClan.values().removeIf(id -> id.equals(clanId));
    }

    /**
     * Returns the owning clan for a chunk, if claimed.
     *
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the owning clan's UUID, or empty if the chunk is unclaimed
     */
    public Optional<UUID> get(String world, int chunkX, int chunkZ) {
        return Optional.ofNullable(chunkToClan.get(key(world, chunkX, chunkZ)));
    }

    /**
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return {@code true} if this chunk is claimed by any clan
     */
    public boolean isClaimed(String world, int chunkX, int chunkZ) {
        return chunkToClan.containsKey(key(world, chunkX, chunkZ));
    }

    public static String key(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }
}
