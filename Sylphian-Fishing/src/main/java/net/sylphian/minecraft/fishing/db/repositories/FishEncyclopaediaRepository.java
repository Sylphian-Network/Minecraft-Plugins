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

/**
 * Implementation of IFishEncyclopaediaRepository using JDBI.
 * Handles database operations asynchronously using a dedicated executor service
 * to prevent blocking the main Minecraft server thread.
 */
public class FishEncyclopaediaRepository implements IFishEncyclopaediaRepository {

    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * Constructs a new FishEncyclopaediaRepository.
     *
     * @param jdbi     the JDBI instance for database access
     * @param executor the executor service for async operations
     */
    public FishEncyclopaediaRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    /**
     * Finds a fish entry for a player.
     * Executes asynchronously on the database executor.
     *
     * @param uuid   the player's UUID
     * @param fishId the ID of the fish
     * @return a CompletableFuture with the Optional result
     */
    @Override
    public CompletableFuture<Optional<FishEncyclopaediaModel>> findEntry(UUID uuid, String fishId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(FishEncyclopaediaDao.class, dao ->
                        dao.findEntry(uuid.toString(), fishId).map(this::toModel)
                ), executor);
    }

    /**
     * Retrieves all caught fish entries for a player.
     * Executes asynchronously on the database executor.
     *
     * @param uuid the player's UUID
     * @return a CompletableFuture with the list of entries
     */
    @Override
    public CompletableFuture<List<FishEncyclopaediaModel>> findAllForPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(FishEncyclopaediaDao.class, dao ->
                        dao.findAllForPlayer(uuid.toString()).stream()
                                .map(this::toModel)
                                .toList()
                ), executor);
    }

    /**
     * Atomically records or updates a fish catch record using an upsert.
     * Executes asynchronously on the database executor.
     *
     * @param uuid   the player's UUID
     * @param fishId the ID of the fish
     * @param weight the weight of the catch
     * @return a CompletableFuture that completes when the record is saved
     */
    @Override
    public CompletableFuture<Void> recordCatch(UUID uuid, String fishId, double weight) {
        long now = Instant.now().getEpochSecond();
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(FishEncyclopaediaDao.class, dao ->
                        dao.upsert(uuid.toString(), fishId, weight, now)
                ), executor);
    }

    /**
     * Maps a database row record to the domain model.
     *
     * @param row the database row
     * @return the mapped FishEncyclopaediaModel
     */
    private FishEncyclopaediaModel toModel(FishEncyclopaediaDao.FishEncyclopaediaRow row) {
        return new FishEncyclopaediaModel(
                UUID.fromString(row.playerUuid()),
                row.fishId(),
                row.timesCaught(),
                row.biggestWeight(),
                row.firstCaught(),
                row.lastCaught()
        );
    }
}