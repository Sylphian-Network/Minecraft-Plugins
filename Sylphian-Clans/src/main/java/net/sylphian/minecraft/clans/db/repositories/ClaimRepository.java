package net.sylphian.minecraft.clans.db.repositories;

import net.sylphian.minecraft.clans.db.api.IClaimRepository;
import net.sylphian.minecraft.clans.db.dao.ClaimDao;
import net.sylphian.minecraft.clans.db.models.ClaimModel;
import org.jdbi.v3.core.Jdbi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * JDBI-backed implementation of {@link IClaimRepository}.
 *
 * <p>All blocking DB calls are dispatched to the shared database executor so the
 * main thread is never blocked.</p>
 */
public class ClaimRepository implements IClaimRepository {

    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * @param jdbi     the JDBI instance for database access
     * @param executor the shared database executor for async dispatch
     */
    public ClaimRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Void> insertClaim(ClaimModel model) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClaimDao.class, dao ->
                        dao.insertClaim(
                                model.world(),
                                model.chunkX(),
                                model.chunkZ(),
                                model.clanId().toString(),
                                model.claimedAt().getEpochSecond()
                        )), executor);
    }

    @Override
    public CompletableFuture<Void> deleteClaim(String world, int chunkX, int chunkZ) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClaimDao.class, dao ->
                        dao.deleteClaim(world, chunkX, chunkZ)), executor);
    }

    @Override
    public CompletableFuture<Void> deleteAllClaimsForClan(UUID clanId) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClaimDao.class, dao ->
                        dao.deleteAllClaimsForClan(clanId.toString())), executor);
    }

    @Override
    public CompletableFuture<Optional<ClaimModel>> findClaimByChunk(String world, int chunkX, int chunkZ) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClaimDao.class, dao ->
                        dao.findClaimByChunk(world, chunkX, chunkZ).map(this::toModel)), executor);
    }

    @Override
    public CompletableFuture<List<ClaimModel>> findClaimsByClan(UUID clanId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClaimDao.class, dao ->
                        dao.findClaimsByClan(clanId.toString()).stream()
                                .map(this::toModel).toList()), executor);
    }

    @Override
    public CompletableFuture<List<ClaimModel>> findAllClaims() {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClaimDao.class, dao ->
                        dao.findAllClaims().stream()
                                .map(this::toModel).toList()), executor);
    }

    private ClaimModel toModel(ClaimDao.ClaimRow row) {
        return new ClaimModel(
                row.world(),
                row.chunkX(),
                row.chunkZ(),
                UUID.fromString(row.clanId()),
                Instant.ofEpochSecond(row.claimedAt())
        );
    }
}
