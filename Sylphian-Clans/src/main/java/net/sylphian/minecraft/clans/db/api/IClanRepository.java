package net.sylphian.minecraft.clans.db.api;

import net.sylphian.minecraft.clans.db.models.ClanMemberModel;
import net.sylphian.minecraft.clans.db.models.ClanModel;
import net.sylphian.minecraft.clans.model.ClanPermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Async persistence contract for clans, members, and member permissions.
 *
 * <p>All methods return {@link CompletableFuture} and execute on the shared
 * database executor. Callers must never block the main thread on these futures.</p>
 */
public interface IClanRepository {

    /**
     * Persists a new clan.
     *
     * @param model the clan to insert
     * @return a future that completes when the row is written
     */
    CompletableFuture<Void> insertClan(ClanModel model);

    /**
     * Deletes a clan and all its member rows (via cascade).
     *
     * @param clanId the clan to delete
     * @return a future that completes when the row is deleted
     */
    CompletableFuture<Void> deleteClan(UUID clanId);

    /**
     * @param clanId the clan's UUID
     * @return a future of the clan row, or empty if not found
     */
    CompletableFuture<Optional<ClanModel>> findClanById(UUID clanId);

    /**
     * @param name the clan's display name (case-sensitive)
     * @return a future of the clan row, or empty if not found
     */
    CompletableFuture<Optional<ClanModel>> findClanByName(String name);

    /**
     * @return a future of all clans ordered by name
     */
    CompletableFuture<List<ClanModel>> findAllClans();

    /**
     * Inserts a member row.
     *
     * @param model the member to insert
     * @return a future that completes when the row is written
     */
    CompletableFuture<Void> insertMember(ClanMemberModel model);

    /**
     * Deletes a member row by player UUID.
     *
     * @param playerUuid the player to remove
     * @return a future that completes when the row is deleted
     */
    CompletableFuture<Void> deleteMember(UUID playerUuid);

    /**
     * @param playerUuid the player's UUID
     * @return a future of the member row, or empty if the player is not in any clan
     */
    CompletableFuture<Optional<ClanMemberModel>> findMemberByPlayer(UUID playerUuid);

    /**
     * @param clanId the clan's UUID
     * @return a future of all member rows for this clan
     */
    CompletableFuture<List<ClanMemberModel>> findMembersByClan(UUID clanId);

    /**
     * Sets the leader flag for a member.
     *
     * @param playerUuid the member's UUID
     * @param isLeader   the new leader flag value
     * @return a future that completes when the row is updated
     */
    CompletableFuture<Void> setLeader(UUID playerUuid, boolean isLeader);

    /**
     * Atomically transfers leadership from one member to another in a single transaction.
     *
     * @param oldLeaderUuid the current leader's UUID
     * @param newLeaderUuid the incoming leader's UUID
     * @return a future that completes when both rows are updated
     */
    CompletableFuture<Void> transferLeader(UUID oldLeaderUuid, UUID newLeaderUuid);

    /**
     * Grants a permission to a member. Silently ignored if already present.
     *
     * @param playerUuid the member's UUID
     * @param permission the permission to grant
     * @return a future that completes when the row is written
     */
    CompletableFuture<Void> insertPermission(UUID playerUuid, ClanPermission permission);

    /**
     * Revokes a specific permission from a member.
     *
     * @param playerUuid the member's UUID
     * @param permission the permission to revoke
     * @return a future that completes when the row is deleted
     */
    CompletableFuture<Void> deletePermission(UUID playerUuid, ClanPermission permission);

    /**
     * Removes all permission rows for a player.
     *
     * @param playerUuid the player's UUID
     * @return a future that completes when all rows are deleted
     */
    CompletableFuture<Void> deleteAllPermissionsForPlayer(UUID playerUuid);

    /**
     * @param playerUuid the player's UUID
     * @return a future of all permissions currently held by the player
     */
    CompletableFuture<List<ClanPermission>> findPermissionsForPlayer(UUID playerUuid);
}
