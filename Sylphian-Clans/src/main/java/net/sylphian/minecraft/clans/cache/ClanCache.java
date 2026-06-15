package net.sylphian.minecraft.clans.cache;

import net.sylphian.minecraft.clans.model.Clan;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache of clan snapshots, keyed on player UUID.
 *
 * <p>Supports synchronous lookups on the main thread. Treat cached values as
 * render snapshots; the database is the source of truth.</p>
 */
public class ClanCache {

    private final Map<UUID, Clan> playerToClan = new ConcurrentHashMap<>();

    /**
     * Stores or updates the clan snapshot for each of its members.
     *
     * @param clan the updated clan snapshot
     */
    public void put(Clan clan) {
        clan.members().forEach(member -> playerToClan.put(member.playerId(), clan));
    }

    /**
     * Removes the cache entry for a single player.
     *
     * @param playerUuid the player whose entry to remove
     */
    public void invalidate(UUID playerUuid) {
        playerToClan.remove(playerUuid);
    }

    /**
     * Removes cache entries for all members of a clan. Used when a clan is disbanded.
     *
     * @param clan the clan whose members to evict
     */
    public void invalidateAll(Clan clan) {
        clan.members().forEach(member -> playerToClan.remove(member.playerId()));
    }

    /**
     * Returns the cached clan for a player, if present.
     *
     * @param playerUuid the player to look up
     * @return the clan snapshot, or empty if not cached
     */
    public Optional<Clan> get(UUID playerUuid) {
        return Optional.ofNullable(playerToClan.get(playerUuid));
    }

    /**
     * Returns the cached clan for a player, or {@code null} if not cached.
     * Prefer {@link #get(UUID)} unless a null return is explicitly needed.
     *
     * @param playerUuid the player to look up
     * @return the clan snapshot, or {@code null}
     */
    public @Nullable Clan getOrNull(UUID playerUuid) {
        return playerToClan.get(playerUuid);
    }
}
