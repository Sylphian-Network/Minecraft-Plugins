package net.sylphian.minecraft.clans.db.api;

import net.sylphian.minecraft.clans.db.models.ClaimModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Async persistence contract for territory claims.
 *
 * <p>All methods return {@link CompletableFuture} and execute on the shared
 * database executor. Callers must never block the main thread on these futures.</p>
 */
public interface IClaimRepository {

    /**
     * Persists a new territory claim.
     *
     * @param model the claim to insert
     * @return a future that completes when the row is written
     */
    CompletableFuture<Void> insertClaim(ClaimModel model);

    /**
     * Persists many claims in a single transaction.
     *
     * @param claims the claims to insert; a no-op if empty
     * @return a future that completes when all rows are written
     */
    CompletableFuture<Void> insertClaims(List<ClaimModel> claims);

    /**
     * Deletes the claim for a specific chunk.
     *
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return a future that completes when the row is deleted
     */
    CompletableFuture<Void> deleteClaim(String world, int chunkX, int chunkZ);

    /**
     * Deletes all claims owned by a clan. Used when a clan is disbanded.
     *
     * @param clanId the owning clan's UUID
     * @return a future that completes when all rows are deleted
     */
    CompletableFuture<Void> deleteAllClaimsForClan(UUID clanId);

    /**
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return a future of the claim, or empty if the chunk is unclaimed
     */
    CompletableFuture<Optional<ClaimModel>> findClaimByChunk(String world, int chunkX, int chunkZ);

    /**
     * @param clanId the owning clan's UUID
     * @return a future of all claims owned by this clan
     */
    CompletableFuture<List<ClaimModel>> findClaimsByClan(UUID clanId);

    /**
     * @return a future of all claims across all clans
     */
    CompletableFuture<List<ClaimModel>> findAllClaims();
}
