package net.sylphian.minecraft.cooking.db.repositories;

import net.sylphian.minecraft.cooking.db.api.ICookingMasteryRepository;
import net.sylphian.minecraft.cooking.db.dao.CookingMasteryDao;
import net.sylphian.minecraft.cooking.db.models.CookingMasteryModel;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * JDBI-backed implementation of {@link ICookingMasteryRepository}.
 * All operations are non-blocking, dispatched to the shared DB executor.
 */
public class CookingMasteryRepository implements ICookingMasteryRepository {

    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * @param jdbi     the JDBI instance
     * @param executor the shared async DB executor
     */
    public CookingMasteryRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Optional<CookingMasteryModel>> findEntry(UUID playerUuid, String recipeId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(CookingMasteryDao.class, dao ->
                        dao.findEntry(playerUuid.toString(), recipeId).map(this::toModel)
                ), executor);
    }

    @Override
    public CompletableFuture<List<CookingMasteryModel>> findAllForPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(CookingMasteryDao.class, dao ->
                        dao.findAllForPlayer(playerUuid.toString()).stream()
                                .map(this::toModel)
                                .toList()
                ), executor);
    }

    @Override
    public CompletableFuture<Void> incrementCount(UUID playerUuid, String recipeId) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(CookingMasteryDao.class, dao ->
                        dao.incrementCount(playerUuid.toString(), recipeId)
                ), executor);
    }

    private CookingMasteryModel toModel(CookingMasteryDao.MasteryRow row) {
        return new CookingMasteryModel(
                UUID.fromString(row.playerUuid()),
                row.recipeId(),
                row.cookCount()
        );
    }
}
