package net.sylphian.minecraft.clans.db.repositories;

import net.sylphian.minecraft.clans.db.api.IClanRepository;
import net.sylphian.minecraft.clans.db.dao.ClanDao;
import net.sylphian.minecraft.clans.db.models.ClanMemberModel;
import net.sylphian.minecraft.clans.db.models.ClanModel;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * JDBI-backed implementation of {@link IClanRepository}.
 *
 * <p>All blocking DB calls are dispatched to the shared database executor so the
 * main thread is never blocked.</p>
 */
public class ClanRepository implements IClanRepository {

    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * @param jdbi     the JDBI instance for database access
     * @param executor the shared database executor for async dispatch
     */
    public ClanRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Void> insertClan(ClanModel model) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.insertClan(
                                model.clanId().toString(),
                                model.name(),
                                model.tag(),
                                model.createdAt()
                        )), executor);
    }

    @Override
    public CompletableFuture<Void> deleteClan(UUID clanId) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.deleteClan(clanId.toString())), executor);
    }

    @Override
    public CompletableFuture<Optional<ClanModel>> findClanById(UUID clanId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findClanById(clanId.toString()).map(this::toClanModel)), executor);
    }

    @Override
    public CompletableFuture<Optional<ClanModel>> findClanByName(String name) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findClanByName(name).map(this::toClanModel)), executor);
    }

    @Override
    public CompletableFuture<Optional<ClanModel>> findClanByTag(String tag) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findClanByTag(tag).map(this::toClanModel)), executor);
    }

    @Override
    public CompletableFuture<List<ClanModel>> findAllClans() {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findAllClans().stream().map(this::toClanModel).toList()), executor);
    }

    @Override
    public CompletableFuture<Void> insertMember(ClanMemberModel model) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.insertMember(
                                model.playerUuid().toString(),
                                model.clanId().toString(),
                                model.isLeader(),
                                model.joinedAt()
                        )), executor);
    }

    @Override
    public CompletableFuture<Void> deleteMember(UUID playerUuid) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.deleteMember(playerUuid.toString())), executor);
    }

    @Override
    public CompletableFuture<Optional<ClanMemberModel>> findMemberByPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findMemberByPlayer(playerUuid.toString()).map(this::toMemberModel)), executor);
    }

    @Override
    public CompletableFuture<List<ClanMemberModel>> findMembersByClan(UUID clanId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findMembersByClan(clanId.toString()).stream()
                                .map(this::toMemberModel).toList()), executor);
    }

    @Override
    public CompletableFuture<Void> setLeader(UUID playerUuid, boolean isLeader) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.setLeader(playerUuid.toString(), isLeader)), executor);
    }

    @Override
    public CompletableFuture<Void> transferLeader(UUID oldLeaderUuid, UUID newLeaderUuid) {
        return CompletableFuture.runAsync(() ->
                jdbi.useTransaction(handle -> {
                    handle.createUpdate("UPDATE clan_members SET is_leader = false WHERE player_uuid = :uuid")
                            .bind("uuid", oldLeaderUuid.toString()).execute();
                    handle.createUpdate("UPDATE clan_members SET is_leader = true WHERE player_uuid = :uuid")
                            .bind("uuid", newLeaderUuid.toString()).execute();
                }), executor);
    }

    @Override
    public CompletableFuture<Void> insertPermission(UUID playerUuid, ClanPermission permission) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.insertPermission(playerUuid.toString(), permission.name())), executor);
    }

    @Override
    public CompletableFuture<Void> deletePermission(UUID playerUuid, ClanPermission permission) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.deletePermission(playerUuid.toString(), permission.name())), executor);
    }

    @Override
    public CompletableFuture<Void> deleteAllPermissionsForPlayer(UUID playerUuid) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.deleteAllPermissionsForPlayer(playerUuid.toString())), executor);
    }

    @Override
    public CompletableFuture<List<ClanPermission>> findPermissionsForPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findPermissionsForPlayer(playerUuid.toString()).stream()
                                .map(ClanPermission::valueOf)
                                .toList()), executor);
    }

    private ClanModel toClanModel(ClanDao.ClanRow row) {
        return new ClanModel(
                UUID.fromString(row.clanId()),
                row.name(),
                row.tag(),
                row.createdAt()
        );
    }

    private ClanMemberModel toMemberModel(ClanDao.MemberRow row) {
        return new ClanMemberModel(
                UUID.fromString(row.playerUuid()),
                UUID.fromString(row.clanId()),
                row.isLeader(),
                row.joinedAt()
        );
    }
}
