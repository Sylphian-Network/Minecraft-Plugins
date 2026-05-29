package net.sylphian.minecraft.fishing.db.api;

import net.sylphian.minecraft.fishing.db.models.FishEncyclopaediaModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IFishEncyclopaediaRepository {
    CompletableFuture<Optional<FishEncyclopaediaModel>> findEntry(UUID uuid, String fishId);
    CompletableFuture<List<FishEncyclopaediaModel>> findAllForPlayer(UUID uuid);
    CompletableFuture<Void> recordCatch(UUID uuid, String fishId, double weight);
}