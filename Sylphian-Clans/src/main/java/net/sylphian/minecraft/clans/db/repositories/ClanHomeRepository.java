package net.sylphian.minecraft.clans.db.repositories;

import net.sylphian.minecraft.clans.db.api.IClanHomeRepository;
import net.sylphian.minecraft.clans.db.dao.ClanHomeDao;
import net.sylphian.minecraft.clans.db.models.ClanHomeModel;
import org.jdbi.v3.core.Jdbi;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * JDBI-backed implementation of {@link IClanHomeRepository}.
 *
 * <p>All blocking DB calls are dispatched to the shared database executor so the
 * main thread is never blocked.</p>
 */
public class ClanHomeRepository implements IClanHomeRepository {

    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * @param jdbi     the JDBI instance for database access
     * @param executor the shared database executor for async dispatch
     */
    public ClanHomeRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Void> setHome(ClanHomeModel model) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanHomeDao.class, dao ->
                        dao.upsertHome(
                                model.clanId().toString(),
                                model.world(),
                                model.x(),
                                model.y(),
                                model.z(),
                                model.yaw(),
                                model.pitch()
                        )), executor);
    }

    @Override
    public CompletableFuture<Optional<ClanHomeModel>> getHome(UUID clanId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanHomeDao.class, dao ->
                        dao.findHomeByClan(clanId.toString()).map(this::toModel)), executor);
    }

    @Override
    public CompletableFuture<Void> deleteHome(UUID clanId) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanHomeDao.class, dao ->
                        dao.deleteHome(clanId.toString())), executor);
    }

    private ClanHomeModel toModel(ClanHomeDao.HomeRow row) {
        return new ClanHomeModel(
                UUID.fromString(row.clanId()),
                row.world(),
                row.x(),
                row.y(),
                row.z(),
                row.yaw(),
                row.pitch()
        );
    }
}
