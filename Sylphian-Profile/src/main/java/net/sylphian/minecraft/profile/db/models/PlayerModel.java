package net.sylphian.minecraft.profile.db.models;

import java.util.UUID;

/**
 * Database model representing a record in the sylphian_profile_players table.
 *
 * @param uuid           the player's Mojang UUID
 * @param xfUserId       linked XenForo user ID (null if not linked)
 * @param mcUsername     the player's Minecraft name
 * @param forumUsername  the player's forum name (null if not linked)
 * @param firstJoined    epoch timestamp of first join
 * @param lastSeen       epoch timestamp of last seen
 * @param playtime       cumulative playtime in seconds
 * @param isOnline      whether the player is currently marked as online in the database
 */
public record PlayerModel(
        UUID uuid,
        Integer xfUserId,
        String mcUsername,
        String forumUsername,
        long firstJoined,
        long lastSeen,
        long playtime,
        boolean isOnline
) {}
