package net.sylphian.minecraft.profile.utils;

import net.sylphian.minecraft.profile.UserProfile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the in-memory cache of online player profiles.
 * Uses a ConcurrentHashMap to ensure thread-safety when profiles are
 * accessed from different threads (e.g., async database callbacks or chat events).
 */
public class ProfileManager {
    /** Internal map of UUID to UserProfile for active players. */
    private final Map<UUID, UserProfile> profiles = new ConcurrentHashMap<>();

    /**
     * Caches a profile in memory.
     * @param profile the profile to cache
     */
    public void cacheProfile(UserProfile profile) {
        profiles.put(profile.uuid(), profile);
    }

    /**
     * Retrieves a cached profile by UUID.
     * @param uuid the player's UUID
     * @return the cached profile, or null if not found
     */
    public UserProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    /**
     * Removes a profile from the cache.
     * @param uuid the player's UUID
     */
    public void invalidate(UUID uuid) {
        profiles.remove(uuid);
    }
}
