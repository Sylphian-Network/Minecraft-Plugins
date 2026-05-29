package net.sylphian.minecraft.profile.db.api;

import net.sylphian.minecraft.profile.db.models.SessionModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ISessionRepository {
    CompletableFuture<Optional<SessionModel>> findById(int sessionId);
    CompletableFuture<List<SessionModel>> findByUuid(UUID uuid);
    CompletableFuture<Optional<SessionModel>> findOpenByUuid(UUID uuid);
    CompletableFuture<Integer> open(UUID uuid, long joinedAt);
    CompletableFuture<Void> close(int sessionId, long quitAt, long duration);
    CompletableFuture<Void> update(SessionModel session);
}
