package net.sylphian.minecraft.profile.db.repositories;

import net.sylphian.minecraft.profile.db.api.IPlayerRepository;
import net.sylphian.minecraft.profile.db.dao.PlayerDao;
import net.sylphian.minecraft.profile.db.models.PlayerModel;
import org.jdbi.v3.core.Jdbi;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository implementation for player data using JDBI.
 * Wraps blocking database calls in CompletableFuture and executes them
 * on a dedicated database thread pool.
 */
public class PlayerRepository implements IPlayerRepository {
    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * Constructs a new PlayerRepository.
     *
     * @param jdbi     the JDBI instance for database access
     * @param executor the executor service for async operations
     */
    public PlayerRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Optional<PlayerModel>> findByUuid(UUID uuid) {
        // Asynchronously query the database for a player by UUID
        return CompletableFuture.supplyAsync(() ->
            jdbi.withExtension(PlayerDao.class, dao ->
                dao.findByUuid(uuid.toString()).map(this::toModel)
            ), executor);
    }

    @Override
    public CompletableFuture<Optional<PlayerModel>> findByXfUserId(Integer xfUserId) {
        // Asynchronously query the database for a player by XenForo ID
        return CompletableFuture.supplyAsync(() ->
            jdbi.withExtension(PlayerDao.class, dao ->
                dao.findByXfUserId(xfUserId).map(this::toModel)
            ), executor);
    }

    @Override
    public CompletableFuture<Void> insert(PlayerModel player) {
        // Asynchronously insert a new player record
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(PlayerDao.class, dao ->
                        dao.insert(
                                player.uuid().toString(),
                                player.xfUserId(),
                                player.mcUsername(),
                                player.forumUsername(),
                                player.firstJoined(),
                                player.lastSeen(),
                                player.playtime(),
                                player.isOnline()
                        )
                ), executor);
    }

    @Override
    public CompletableFuture<Void> update(PlayerModel player) {
        // Asynchronously update an existing player record
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(PlayerDao.class, dao ->
                        dao.update(
                                player.uuid().toString(),
                                player.xfUserId(),
                                player.mcUsername(),
                                player.forumUsername(),
                                player.lastSeen(),
                                player.playtime(),
                                player.isOnline()
                        )
                ), executor);
    }

    /**
     * Maps a JDBI row object to a domain model.
     * @param row the database row
     * @return the player model
     */
    private PlayerModel toModel(PlayerDao.PlayerRow row) {
        return new PlayerModel(
                UUID.fromString(row.uuid()),
                row.xfUserId(),
                row.mcUsername(),
                row.forumUsername(),
                row.firstJoined(),
                row.lastSeen(),
                row.playtime(),
                row.isOnline()
        );
    }
}
