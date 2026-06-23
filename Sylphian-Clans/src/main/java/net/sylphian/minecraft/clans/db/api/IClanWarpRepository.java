package net.sylphian.minecraft.clans.db.api;

import net.sylphian.minecraft.clans.db.models.ClanWarpModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Async persistence contract for clan warps and their per-warp access lists.
 *
 * <p>All methods return {@link CompletableFuture} and execute on the shared
 * database executor. Callers must never block the main thread on these futures.</p>
 */
public interface IClanWarpRepository {

    /**
     * Inserts a warp, or updates the location, icon, and description of an existing one.
     * Does not change the {@code restricted} flag.
     *
     * @param model the warp to persist
     * @return a future that completes when the row is written
     */
    CompletableFuture<Void> saveWarp(ClanWarpModel model);

    /**
     * @param clanId the owning clan
     * @param name   the warp name
     * @return a future of the warp, or empty if none exists with that name
     */
    CompletableFuture<Optional<ClanWarpModel>> getWarp(UUID clanId, String name);

    /**
     * @param clanId the owning clan
     * @return a future of all warps the clan owns, ordered by name
     */
    CompletableFuture<List<ClanWarpModel>> listWarps(UUID clanId);

    /**
     * @param clanId the owning clan
     * @return a future of the number of warps the clan owns
     */
    CompletableFuture<Integer> countWarps(UUID clanId);

    /**
     * Removes a warp. Its access rows are removed by the database cascade.
     *
     * @param clanId the owning clan
     * @param name   the warp name
     * @return a future that completes when the row is deleted
     */
    CompletableFuture<Void> deleteWarp(UUID clanId, String name);

    /**
     * Sets whether a warp is restricted to its access list.
     *
     * @param clanId     the owning clan
     * @param name       the warp name
     * @param restricted the new restriction state
     * @return a future that completes when the change is persisted
     */
    CompletableFuture<Void> setRestricted(UUID clanId, String name, boolean restricted);

    /**
     * Grants a member access to a warp. Idempotent.
     *
     * @param clanId   the owning clan
     * @param name     the warp name
     * @param playerId the member to grant
     * @return a future that completes when the access row is written
     */
    CompletableFuture<Void> grantAccess(UUID clanId, String name, UUID playerId);

    /**
     * Revokes a member's access to a warp. No-op if not granted.
     *
     * @param clanId   the owning clan
     * @param name     the warp name
     * @param playerId the member to revoke
     * @return a future that completes when the access row is removed
     */
    CompletableFuture<Void> revokeAccess(UUID clanId, String name, UUID playerId);

    /**
     * @param clanId   the owning clan
     * @param name     the warp name
     * @param playerId the member to test
     * @return a future that is {@code true} if the member has access to the warp
     */
    CompletableFuture<Boolean> hasAccess(UUID clanId, String name, UUID playerId);

    /**
     * @param clanId the owning clan
     * @param name   the warp name
     * @return a future of the UUIDs of all members granted access to the warp
     */
    CompletableFuture<List<UUID>> listAccess(UUID clanId, String name);

    /**
     * @param clanId   the owning clan
     * @param playerId the member to look up
     * @return a future of the names of every warp the member has been granted access to
     */
    CompletableFuture<List<String>> listAccessibleWarps(UUID clanId, UUID playerId);
}
