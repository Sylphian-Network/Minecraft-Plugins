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

public class SessionRepository implements ISessionRepository {
    private final Jdbi jdbi;
    private final ExecutorService executor;

    public SessionRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Optional<SessionModel>> findById(int sessionId) {
        return CompletableFuture.supplyAsync(() ->
            jdbi.withExtension(SessionDao.class, dao ->
                dao.findById(sessionId).map(this::toModel)
            ), executor);
    }

    @Override
    public CompletableFuture<List<SessionModel>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
            jdbi.withExtension(SessionDao.class, dao ->
                dao.findByUuid(uuid.toString()).stream()
                    .map(this::toModel)
                    .collect(Collectors.toList())
            ), executor);
    }

    @Override
    public CompletableFuture<Optional<SessionModel>> findOpenByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(SessionDao.class, dao ->
                        dao.findOpenByUuid(uuid.toString()).map(this::toModel)
                ), executor);
    }

    @Override
    public CompletableFuture<Integer> open(UUID uuid, long joinedAt) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(SessionDao.class, dao ->
                        dao.open(uuid.toString(), joinedAt)
                ), executor);
    }

    @Override
    public CompletableFuture<Void> close(int sessionId, long quitAt, long duration) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(SessionDao.class, dao ->
                        dao.close(sessionId, quitAt, duration)
                ), executor);
    }

    @Override
    public CompletableFuture<Void> update(SessionModel session) {
        return CompletableFuture.runAsync(() ->
            jdbi.useExtension(SessionDao.class, dao ->
                dao.update(session.sessionId(), session.quitAt(), session.duration())
            ), executor);
    }

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
