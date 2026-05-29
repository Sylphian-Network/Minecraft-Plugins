package net.sylphian.minecraft.profile.db.models;

import java.util.UUID;

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
