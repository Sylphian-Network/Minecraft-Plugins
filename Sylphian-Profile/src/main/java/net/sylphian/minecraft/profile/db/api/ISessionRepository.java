package net.sylphian.minecraft.profile.db.api;

import net.sylphian.minecraft.profile.db.models.SessionModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for session data persistence operations.
 * Tracks player login/logout times and session durations.
 */
public interface ISessionRepository {
    /**
     * Finds a session by its unique auto-incrementing ID.
     *
     * @param sessionId the session ID
     * @return a future containing an optional session model
     */
    CompletableFuture<Optional<SessionModel>> findById(int sessionId);

    /**
     * Finds all sessions associated with a specific player.
     *
     * @param uuid the player's UUID
     * @return a future containing a list of sessions
     */
    CompletableFuture<List<SessionModel>> findByUuid(UUID uuid);

    /**
     * Finds the currently open session for a player (where quit_at is null).
     *
     * @param uuid the player's UUID
     * @return a future containing an optional session model
     */
    CompletableFuture<Optional<SessionModel>> findOpenByUuid(UUID uuid);

    /**
     * Records the start of a new player session.
     *
     * @param uuid     the player's UUID
     * @param joinedAt epoch timestamp when the player joined
     * @return a future containing the generated session ID
     */
    CompletableFuture<Integer> open(UUID uuid, long joinedAt);

    /**
     * Closes an active session by recording the logout time and total duration.
     *
     * @param sessionId the ID of the session to close
     * @param quitAt    epoch timestamp when the player left
     * @param duration  total duration of the session in seconds
     * @return a future that completes when the session is closed
     */
    CompletableFuture<Void> close(int sessionId, long quitAt, long duration);

    /**
     * Updates an existing session record.
     *
     * @param session the session model with updated data
     * @return a future that completes when the update is finished
     */
    CompletableFuture<Void> update(SessionModel session);
}
