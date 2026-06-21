package net.sylphian.minecraft.clans.db.repositories;

import net.sylphian.minecraft.clans.db.api.IClanWarpRepository;
import net.sylphian.minecraft.clans.db.dao.ClanWarpDao;
import net.sylphian.minecraft.clans.db.models.ClanWarpModel;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * JDBI-backed implementation of {@link IClanWarpRepository}.
 *
 * <p>All blocking DB calls are dispatched to the shared database executor so the
 * main thread is never blocked.</p>
 */
public class ClanWarpRepository implements IClanWarpRepository {

    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * @param jdbi     the JDBI instance for database access
     * @param executor the shared database executor for async dispatch
     */
    public ClanWarpRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Void> saveWarp(ClanWarpModel model) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanWarpDao.class, dao ->
                        dao.upsertWarp(
                                model.clanId().toString(),
                                model.name(),
                                model.world(),
                                model.x(),
                                model.y(),
                                model.z(),
                                model.yaw(),
                                model.pitch(),
                                model.icon(),
                                model.description()
                        )), executor);
    }

    @Override
    public CompletableFuture<Optional<ClanWarpModel>> getWarp(UUID clanId, String name) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanWarpDao.class, dao ->
                        dao.findWarp(clanId.toString(), name).map(this::toModel)), executor);
    }

    @Override
    public CompletableFuture<List<ClanWarpModel>> listWarps(UUID clanId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanWarpDao.class, dao ->
                        dao.findWarpsByClan(clanId.toString()).stream().map(this::toModel).toList()), executor);
    }

    @Override
    public CompletableFuture<Integer> countWarps(UUID clanId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanWarpDao.class, dao -> dao.countWarps(clanId.toString())), executor);
    }

    @Override
    public CompletableFuture<Void> deleteWarp(UUID clanId, String name) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanWarpDao.class, dao -> dao.deleteWarp(clanId.toString(), name)), executor);
    }

    @Override
    public CompletableFuture<Void> setRestricted(UUID clanId, String name, boolean restricted) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanWarpDao.class, dao -> dao.setRestricted(clanId.toString(), name, restricted)), executor);
    }

    @Override
    public CompletableFuture<Void> grantAccess(UUID clanId, String name, UUID playerId) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanWarpDao.class, dao ->
                        dao.insertAccess(clanId.toString(), name, playerId.toString())), executor);
    }

    @Override
    public CompletableFuture<Void> revokeAccess(UUID clanId, String name, UUID playerId) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanWarpDao.class, dao ->
                        dao.deleteAccess(clanId.toString(), name, playerId.toString())), executor);
    }

    @Override
    public CompletableFuture<Boolean> hasAccess(UUID clanId, String name, UUID playerId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanWarpDao.class, dao ->
                        dao.hasAccess(clanId.toString(), name, playerId.toString())), executor);
    }

    @Override
    public CompletableFuture<List<UUID>> listAccess(UUID clanId, String name) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanWarpDao.class, dao ->
                        dao.findAccessByWarp(clanId.toString(), name).stream().map(UUID::fromString).toList()), executor);
    }

    @Override
    public CompletableFuture<List<String>> listAccessibleWarps(UUID clanId, UUID playerId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanWarpDao.class, dao ->
                        dao.findAccessibleWarps(clanId.toString(), playerId.toString())), executor);
    }

    private ClanWarpModel toModel(ClanWarpDao.WarpRow row) {
        return new ClanWarpModel(
                UUID.fromString(row.clanId()),
                row.name(),
                row.world(),
                row.x(),
                row.y(),
                row.z(),
                row.yaw(),
                row.pitch(),
                row.icon(),
                row.description(),
                row.restricted()
        );
    }
}
