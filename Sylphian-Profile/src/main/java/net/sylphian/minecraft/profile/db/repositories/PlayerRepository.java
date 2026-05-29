package net.sylphian.minecraft.profile.db.repositories;

import net.sylphian.minecraft.profile.db.api.IPlayerRepository;
import net.sylphian.minecraft.profile.db.dao.PlayerDao;
import net.sylphian.minecraft.profile.db.models.PlayerModel;
import org.jdbi.v3.core.Jdbi;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class PlayerRepository implements IPlayerRepository {
    private final Jdbi jdbi;
    private final ExecutorService executor;

    public PlayerRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Optional<PlayerModel>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
            jdbi.withExtension(PlayerDao.class, dao ->
                dao.findByUuid(uuid.toString()).map(this::toModel)
            ), executor);
    }

    @Override
    public CompletableFuture<Optional<PlayerModel>> findByXfUserId(Integer xfUserId) {
        return CompletableFuture.supplyAsync(() ->
            jdbi.withExtension(PlayerDao.class, dao ->
                dao.findByXfUserId(xfUserId).map(this::toModel)
            ), executor);
    }

    @Override
    public CompletableFuture<Void> insert(PlayerModel player) {
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
