package net.sylphian.minecraft.profile;

import java.util.List;
import java.util.UUID;

public record UserProfile(
    UUID uuid,
    Integer xfUserId,
    String mcUsername,
    String forumUsername,
    long firstJoined,
    long lastSeen,
    long playtime,
    List<Entitlement> entitlements
) {
    public record Entitlement(
        int cosmeticId,
        String internalKey,
        String type,
        Long expiresAt
    ) {}
}
