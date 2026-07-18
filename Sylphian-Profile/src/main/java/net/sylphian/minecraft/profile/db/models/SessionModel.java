package net.sylphian.minecraft.profile.db.models;

import java.util.UUID;

/**
 * Database model representing a record in the sylphian_profile_sessions table.
 *
 * @param sessionId the unique session ID
 * @param uuid      the player's Mojang UUID
 * @param joinedAt  epoch timestamp when the session started
 * @param quitAt    epoch timestamp when the session ended (null if active)
 * @param duration  total duration of the session in seconds
 */
public record SessionModel(
        int sessionId,
        UUID uuid,
        long joinedAt,
        Long quitAt,
        long duration
) {}