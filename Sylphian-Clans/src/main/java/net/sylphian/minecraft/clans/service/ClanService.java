package net.sylphian.minecraft.clans.service;

import net.sylphian.minecraft.clans.api.ClanAPI;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.db.api.IClanRepository;
import net.sylphian.minecraft.clans.db.models.ClanMemberModel;
import net.sylphian.minecraft.clans.db.models.ClanModel;
import net.sylphian.minecraft.clans.event.*;
import net.sylphian.minecraft.clans.model.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Business logic implementation of {@link ClanAPI}.
 */
public class ClanService implements ClanAPI {

    private final IClanRepository clanRepository;
    private final TerritoryService territoryService;
    private final ClanCache clanCache;
    private final JavaPlugin plugin;
    private List<ClanPermission> defaultMemberPermissions;

    /**
     * @param clanRepository           persistence layer for clans and members
     * @param territoryService         territory logic, used during disbandment
     * @param clanCache                in-memory membership cache
     * @param plugin                   the owning plugin, used for scheduler hops
     * @param defaultMemberPermissions permissions granted to a new member on join
     */
    public ClanService(IClanRepository clanRepository, TerritoryService territoryService,
                       ClanCache clanCache, JavaPlugin plugin,
                       List<ClanPermission> defaultMemberPermissions) {
        this.clanRepository = clanRepository;
        this.territoryService = territoryService;
        this.clanCache = clanCache;
        this.plugin = plugin;
        this.defaultMemberPermissions = defaultMemberPermissions;
    }

    /**
     * Updates the default member permissions applied to new members. Called after a config reload.
     *
     * @param permissions the new default permission list
     */
    public void setDefaultMemberPermissions(List<ClanPermission> permissions) {
        this.defaultMemberPermissions = permissions;
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByPlayer(UUID playerUuid) {
        Optional<Clan> cached = clanCache.get(playerUuid);
        if (cached.isPresent()) return CompletableFuture.completedFuture(cached);

        return clanRepository.findMemberByPlayer(playerUuid).thenCompose(memberOpt -> memberOpt.map(clanMemberModel -> buildClan(clanMemberModel.clanId()).thenApply(Optional::ofNullable)).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
    }

    @Override
    public Optional<Clan> getClanByPlayerCached(UUID playerUuid) {
        return clanCache.get(playerUuid);
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanById(UUID clanId) {
        return buildClan(clanId).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByName(String name) {
        return clanRepository.findClanByName(name).thenCompose(opt -> opt.map(clanModel -> buildClan(clanModel.clanId()).thenApply(Optional::ofNullable)).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
    }

    /**
     * Returns all clans, each fully assembled with members and permissions.
     *
     * @return a future of all clans
     */
    public CompletableFuture<List<Clan>> getAllClans() {
        return clanRepository.findAllClans().thenCompose(models -> {
            List<CompletableFuture<Clan>> futures = models.stream()
                    .map(m -> buildClan(m.clanId()))
                    .toList();
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toList()));
        });
    }

    /**
     * Creates a new clan with the given player as the leader.
     *
     * @param leaderUuid the UUID of the founding player
     * @param name       the clan display name (max 32 chars, alphanumeric/hyphens)
     * @param tag        the clan tag (max 6 chars, alphanumeric)
     * @return a future that completes when the clan is created
     * @throws IllegalArgumentException if the name or tag is already taken, or fails validation
     */
    public CompletableFuture<Void> createClan(UUID leaderUuid, String name, String tag) {
        validateName(name);
        validateTag(tag);

        return clanRepository.findClanByName(name).thenCompose(existing -> {
            if (existing.isPresent()) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("A clan named '" + name + "' already exists."));
            }
            return clanRepository.findClanByTag(tag);
        }).thenCompose(existingTag -> {
            if (existingTag.isPresent()) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("The tag '" + tag + "' is already in use."));
            }

            UUID clanId = UUID.randomUUID();
            long now = Instant.now().getEpochSecond();

            ClanModel clanModel = new ClanModel(clanId, name, tag, now);
            ClanMemberModel memberModel = new ClanMemberModel(leaderUuid, clanId, true, now);

            return clanRepository.insertClan(clanModel)
                    .thenCompose(v -> clanRepository.insertMember(memberModel))
                    .thenCompose(v -> buildClan(clanId))
                    .thenAccept(clan -> {
                        if (clan != null) clanCache.put(clan);
                        fireEvent(new ClanCreateEvent(clanId));
                    });
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            if (msg.contains("duplicate") || msg.contains("unique")) {
                throw new RuntimeException("That name or tag was just taken. Please try again.");
            }
            throw ex instanceof RuntimeException re ? re : new RuntimeException(cause);
        });
    }

    /**
     * Disbands a clan. Removes all territory claims, clears the cache for all members,
     * and fires {@link ClanDisbandEvent}.
     *
     * @param clanId the clan to disband
     * @return a future that completes when disbandment is fully applied
     */
    public CompletableFuture<Void> disbandClan(UUID clanId) {
        return buildClan(clanId).thenCompose(clan -> {
            if (clan == null) return CompletableFuture.completedFuture(null);

            // Delete permissions for all members before deleting member rows.
            List<CompletableFuture<Void>> permDeletions = clan.members().stream()
                    .map(m -> clanRepository.deleteAllPermissionsForPlayer(m.playerId()))
                    .toList();

            return CompletableFuture.allOf(permDeletions.toArray(new CompletableFuture[0]))
                    .thenCompose(v -> territoryService.unclaimAll(clanId))
                    .thenCompose(v -> clanRepository.deleteClan(clanId))
                    .thenRun(() -> {
                        clanCache.invalidateAll(clan);
                        fireEvent(new ClanDisbandEvent(clanId));
                    });
        });
    }

    /**
     * Adds a player to a clan with the configured default permissions.
     * Clears all their pending invites after a successful join.
     *
     * @param clanId      the clan to join
     * @param playerUuid  the player joining
     * @param inviteService the invite service, used to clear pending invites
     * @return a future that completes when the member is added
     */
    public CompletableFuture<Void> addMember(UUID clanId, UUID playerUuid, ClanInviteService inviteService) {
        long now = Instant.now().getEpochSecond();
        ClanMemberModel model = new ClanMemberModel(playerUuid, clanId, false, now);

        return clanRepository.insertMember(model)
                .thenCompose(v -> {
                    List<CompletableFuture<Void>> permInserts = defaultMemberPermissions.stream()
                            .map(p -> clanRepository.insertPermission(playerUuid, p))
                            .toList();
                    return CompletableFuture.allOf(permInserts.toArray(new CompletableFuture[0]));
                })
                .thenCompose(v -> buildClan(clanId))
                .thenAccept(clan -> {
                    if (clan != null) clanCache.put(clan);
                    inviteService.clearInvites(playerUuid);
                    fireEvent(new ClanMemberJoinEvent(clanId, playerUuid));
                });
    }

    /**
     * Removes a member from their clan (kick or voluntary leave).
     * Deletes their permission rows before removing their member row.
     *
     * @param clanId      the clan the player is leaving
     * @param playerUuid  the player to remove
     * @return a future that completes when the member is removed
     */
    public CompletableFuture<Void> removeMember(UUID clanId, UUID playerUuid) {
        return clanRepository.deleteAllPermissionsForPlayer(playerUuid)
                .thenCompose(v -> clanRepository.deleteMember(playerUuid))
                .thenRun(() -> clanCache.invalidate(playerUuid))
                .thenCompose(v -> buildClan(clanId))
                .thenAccept(clan -> {
                    if (clan != null) clanCache.put(clan);
                    fireEvent(new ClanMemberLeaveEvent(clanId, playerUuid));
                });
    }

    /**
     * Transfers leadership from the current leader to another member.
     * Two role-change events are fired: one for the old leader and one for the new.
     *
     * @param clanId         the clan in which leadership is being transferred
     * @param newLeaderUuid  the member who will become the new leader
     * @return a future that completes when both rows are updated
     */
    public CompletableFuture<Void> transferLeadership(UUID clanId, UUID newLeaderUuid) {
        return getClanById(clanId).thenCompose(clanOpt -> {
            if (clanOpt.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Clan not found."));
            }
            Clan clan = clanOpt.get();
            UUID oldLeader = clan.leaderId().orElseThrow();

            return clanRepository.transferLeader(oldLeader, newLeaderUuid)
                    .thenCompose(v -> buildClan(clanId))
                    .thenAccept(updated -> {
                        if (updated != null) clanCache.put(updated);
                        fireEvent(new ClanRoleChangeEvent(clanId, oldLeader));
                        fireEvent(new ClanRoleChangeEvent(clanId, newLeaderUuid));
                    });
        });
    }

    /**
     * Grants a permission to a clan member.
     *
     * <p>If {@code permission} is a {@code GRANT_*} entry, the requester must be
     * the LEADER. Otherwise the requester must hold {@code permission.asGrant()}.</p>
     *
     * @param requesterId the player attempting the grant
     * @param targetId    the player receiving the permission
     * @param permission  the permission to grant
     * @return a future that completes when the permission is persisted
     * @throws IllegalArgumentException if the requester lacks authority
     */
    public CompletableFuture<Void> grantPermission(UUID requesterId, UUID targetId, ClanPermission permission) {
        return validatePermissionAuthority(requesterId, permission).thenCompose(clan ->
                clanRepository.insertPermission(targetId, permission)
                        .thenCompose(v -> buildClan(clan.clanId()))
                        .thenAccept(updated -> { if (updated != null) clanCache.put(updated); })
        );
    }

    /**
     * Revokes a permission from a clan member. Uses the same authority rules as
     * {@link #grantPermission}.
     *
     * @param requesterId the player attempting the revocation
     * @param targetId    the player losing the permission
     * @param permission  the permission to revoke
     * @return a future that completes when the permission row is deleted
     * @throws IllegalArgumentException if the requester lacks authority
     */
    public CompletableFuture<Void> revokePermission(UUID requesterId, UUID targetId, ClanPermission permission) {
        return validatePermissionAuthority(requesterId, permission).thenCompose(clan ->
                clanRepository.deletePermission(targetId, permission)
                        .thenCompose(v -> buildClan(clan.clanId()))
                        .thenAccept(updated -> { if (updated != null) clanCache.put(updated); })
        );
    }

    /**
     * Loads and caches the clan snapshot for a player, if they are in one.
     * No-op if the player is not a clan member or is already cached.
     *
     * @param playerUuid the joining player's UUID
     * @return a future that completes when the cache is populated
     */
    public CompletableFuture<Void> seedCacheForPlayer(UUID playerUuid) {
        if (clanCache.get(playerUuid).isPresent()) return CompletableFuture.completedFuture(null);
        return getClanByPlayer(playerUuid).thenAccept(opt -> opt.ifPresent(clanCache::put));
    }

    /**
     * Builds a full {@link Clan} snapshot by loading the clan row, all member rows,
     * and each member's permission rows from the database.
     *
     * @param clanId the clan to load
     * @return a future of the assembled snapshot, or {@code null} if not found
     */
    private CompletableFuture<Clan> buildClan(UUID clanId) {
        return clanRepository.findClanById(clanId).thenCompose(clanOpt -> {
            if (clanOpt.isEmpty()) return CompletableFuture.completedFuture(null);
            ClanModel model = clanOpt.get();

            return clanRepository.findMembersByClan(clanId).thenCompose(memberModels -> {
                List<CompletableFuture<ClanMember>> memberFutures = memberModels.stream()
                        .map(m -> clanRepository.findPermissionsForPlayer(m.playerUuid())
                                .thenApply(perms -> new ClanMember(
                                        m.playerUuid(),
                                        m.isLeader() ? ClanRole.LEADER : ClanRole.MEMBER,
                                        new HashSet<>(perms),
                                        Instant.ofEpochSecond(m.joinedAt())
                                )))
                        .toList();

                return CompletableFuture.allOf(memberFutures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> {
                            List<ClanMember> members = memberFutures.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toList());
                            return new Clan(
                                    model.clanId(),
                                    model.name(),
                                    model.tag(),
                                    members,
                                    Instant.ofEpochSecond(model.createdAt())
                            );
                        });
            });
        });
    }

    /**
     * Validates that {@code requesterId} has authority to assign or revoke
     * {@code permission}, and returns their clan.
     *
     * @param requesterId the requesting player
     * @param permission  the permission being assigned or revoked
     * @return a future of the requester's clan
     * @throws IllegalArgumentException if the requester lacks authority or is not in a clan
     */
    private CompletableFuture<Clan> validatePermissionAuthority(UUID requesterId, ClanPermission permission) {
        return getClanByPlayer(requesterId).thenApply(clanOpt -> {
            if (clanOpt.isEmpty()) {
                throw new IllegalArgumentException("You are not in a clan.");
            }
            Clan clan = clanOpt.get();
            ClanPermission required = permission.isGrant()
                    ? null  // only LEADER may assign GRANT_* permissions
                    : permission.asGrant();

            if (required == null) {
                if (!clan.leaderId().map(requesterId::equals).orElse(false)) {
                    throw new IllegalArgumentException("Only the clan leader may assign GRANT_* permissions.");
                }
            } else {
                if (!clan.hasPermission(requesterId, required)) {
                    throw new IllegalArgumentException(
                            "You need " + required.name() + " to assign " + permission.name() + ".");
                }
            }
            return clan;
        });
    }

    /**
     * Validates a clan name: 3–32 characters, letters, digits, spaces, and hyphens.
     *
     * @param name the name to validate
     * @throws IllegalArgumentException if the name fails validation
     */
    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() < 3 || name.length() > 32
                || !name.matches("[a-zA-Z0-9 \\-]+")) {
            throw new IllegalArgumentException(
                    "Clan name must be 3–32 characters and contain only letters, digits, spaces, or hyphens.");
        }
    }

    /**
     * Validates a clan tag: 2–6 characters, letters and digits only.
     *
     * @param tag the tag to validate
     * @throws IllegalArgumentException if the tag fails validation
     */
    private void validateTag(String tag) {
        if (tag == null || tag.isBlank() || tag.length() < 2 || tag.length() > 6 || !tag.matches("[a-zA-Z0-9]+")) {
            throw new IllegalArgumentException("Clan tag must be 2–6 characters and contain only letters and digits.");
        }
    }

    /** Fires a Bukkit event on the main thread. */
    private void fireEvent(org.bukkit.event.Event event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(event));
    }
}
