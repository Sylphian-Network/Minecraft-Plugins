package net.sylphian.minecraft.profile.db.models;

import java.util.UUID;

public record SessionModel(
        int sessionId,
        UUID uuid,
        long joinedAt,
        Long quitAt,
        long duration
) {}