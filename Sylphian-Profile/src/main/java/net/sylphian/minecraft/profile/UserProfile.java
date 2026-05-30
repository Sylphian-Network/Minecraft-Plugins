package net.sylphian.minecraft.profile;

import java.util.List;
import java.util.UUID;

/**
 * Represents a cached player profile in memory.
 * This record stores both persistent database data and temporary session information
 * used by various systems like chat prefixes, playtime tracking, and entitlement checks.
 *
 * @param uuid           the player's Mojang UUID
 * @param xfUserId       the linked XenForo user ID, or null if not linked
 * @param mcUsername     the player's current Minecraft username
 * @param forumUsername  the linked forum username, or null if not linked
 * @param firstJoined    epoch timestamp of the player's first join
 * @param lastSeen       epoch timestamp of the player's last seen time
 * @param playtime       cumulative playtime in seconds
 * @param entitlements   list of active entitlements (cosmetics, ranks, etc.)
 */
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
    /**
     * Represents a specific entitlement owned by a player.
     *
     * @param cosmeticId  unique ID of the cosmetic item
     * @param internalKey unique key used for permission or lookups
     * @param type        the type of entitlement (e.g., RANK, PARTICLE, TAG)
     * @param expiresAt   epoch timestamp when this expires, or null if permanent
     */
    public record Entitlement(
        int cosmeticId,
        String internalKey,
        String type,
        Long expiresAt
    ) {}
}
