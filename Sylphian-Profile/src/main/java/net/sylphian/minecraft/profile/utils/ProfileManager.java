package net.sylphian.minecraft.profile.utils;

import net.sylphian.minecraft.profile.UserProfile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManager {
    private final Map<UUID, UserProfile> profiles = new ConcurrentHashMap<>();

    public void cacheProfile(UserProfile profile) {
        profiles.put(profile.uuid(), profile);
    }

    public UserProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    public void invalidate(UUID uuid) {
        profiles.remove(uuid);
    }

    public Map<UUID, UserProfile> getActiveProfiles() {
        return profiles;
    }
}
