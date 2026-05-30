package net.sylphian.minecraft.profile.db.repositories;

import net.sylphian.minecraft.profile.db.api.ISessionRepository;
import net.sylphian.minecraft.profile.db.dao.SessionDao;
import net.sylphian.minecraft.profile.db.models.SessionModel;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Repository implementation for session data using JDBI.
 * Handles tracking and persistence of player session history.
 */
public class SessionRepository implements ISessionRepository {
    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * Constructs a new SessionRepository.
     *
     * @param jdbi     the JDBI instance for database access
     * @param executor the executor service for async operations
     */
    public SessionRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Optional<SessionModel>> findById(int sessionId) {
        // Asynchronously query for a specific session by ID
        return CompletableFuture.supplyAsync(() ->
            jdbi.withExtension(SessionDao.class, dao ->
                dao.findById(sessionId).map(this::toModel)
            ), executor);
    }

    @Override
    public CompletableFuture<List<SessionModel>> findByUuid(UUID uuid) {
        // Asynchronously query for all sessions belonging to a player
        return CompletableFuture.supplyAsync(() ->
            jdbi.withExtension(SessionDao.class, dao ->
                dao.findByUuid(uuid.toString()).stream()
                    .map(this::toModel)
                    .collect(Collectors.toList())
            ), executor);
    }

    @Override
    public CompletableFuture<Optional<SessionModel>> findOpenByUuid(UUID uuid) {
        // Asynchronously query for a player's currently active session
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(SessionDao.class, dao ->
                        dao.findOpenByUuid(uuid.toString()).map(this::toModel)
                ), executor);
    }

    @Override
    public CompletableFuture<Integer> open(UUID uuid, long joinedAt) {
        // Asynchronously record the start of a new session
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(SessionDao.class, dao ->
                        dao.open(uuid.toString(), joinedAt)
                ), executor);
    }

    @Override
    public CompletableFuture<Void> close(int sessionId, long quitAt, long duration) {
        // Asynchronously record the end of a session
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(SessionDao.class, dao ->
                        dao.close(sessionId, quitAt, duration)
                ), executor);
    }

    @Override
    public CompletableFuture<Void> update(SessionModel session) {
        // Asynchronously update session details
        return CompletableFuture.runAsync(() ->
            jdbi.useExtension(SessionDao.class, dao ->
                dao.update(session.sessionId(), session.quitAt(), session.duration())
            ), executor);
    }

    /**
     * Maps a JDBI row object to a domain model.
     * @param row the database row
     * @return the session model
     */
    private SessionModel toModel(SessionDao.SessionRow row) {
        return new SessionModel(
            row.sessionId(),
            UUID.fromString(row.uuid()),
            row.joinedAt(),
            row.quitAt(),
            row.duration()
        );
    }
}
