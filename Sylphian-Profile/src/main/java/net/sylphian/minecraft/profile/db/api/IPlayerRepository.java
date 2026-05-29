package net.sylphian.minecraft.profile.db.api;

import net.sylphian.minecraft.profile.db.models.PlayerModel;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IPlayerRepository {
    CompletableFuture<Optional<PlayerModel>> findByUuid(UUID uuid);
    CompletableFuture<Optional<PlayerModel>> findByXfUserId(Integer xfUserId);
    CompletableFuture<Void> insert(PlayerModel player);
    CompletableFuture<Void> update(PlayerModel player);
}
