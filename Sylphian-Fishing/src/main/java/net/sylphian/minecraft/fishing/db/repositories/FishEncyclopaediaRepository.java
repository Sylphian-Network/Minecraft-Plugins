package net.sylphian.minecraft.fishing.db.repositories;

import net.sylphian.minecraft.fishing.db.api.IFishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.db.dao.FishEncyclopaediaDao;
import net.sylphian.minecraft.fishing.db.models.FishEncyclopaediaModel;
import org.jdbi.v3.core.Jdbi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class FishEncyclopaediaRepository implements IFishEncyclopaediaRepository {

    private final Jdbi jdbi;
    private final ExecutorService executor;

    public FishEncyclopaediaRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Optional<FishEncyclopaediaModel>> findEntry(UUID uuid, String fishId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(FishEncyclopaediaDao.class, dao ->
                        dao.findEntry(uuid.toString(), fishId).map(this::toModel)
                ), executor);
    }

    @Override
    public CompletableFuture<List<FishEncyclopaediaModel>> findAllForPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(FishEncyclopaediaDao.class, dao ->
                        dao.findAllForPlayer(uuid.toString()).stream()
                                .map(this::toModel)
                                .toList()
                ), executor);
    }

    @Override
    public CompletableFuture<Void> recordCatch(UUID uuid, String fishId, double weight) {
        long now = Instant.now().getEpochSecond();
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(FishEncyclopaediaDao.class, dao -> {
                    Optional<FishEncyclopaediaDao.FishEncyclopaediaRow> existing =
                            dao.findEntry(uuid.toString(), fishId);

                    if (existing.isEmpty()) {
                        dao.insert(uuid.toString(), fishId, weight, now);
                    } else {
                        dao.update(uuid.toString(), fishId, weight, now);
                    }
                }), executor);
    }

    private FishEncyclopaediaModel toModel(FishEncyclopaediaDao.FishEncyclopaediaRow row) {
        return new FishEncyclopaediaModel(
                UUID.fromString(row.uuid()),
                row.fishId(),
                row.timesCaught(),
                row.biggestWeight(),
                row.firstCaught(),
                row.lastCaught()
        );
    }
}