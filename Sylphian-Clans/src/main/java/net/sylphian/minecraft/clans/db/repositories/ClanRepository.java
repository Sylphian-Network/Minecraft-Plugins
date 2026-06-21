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
 * main thread is never blocked. All queries are scoped to {@code serverId}.</p>
 */
public class ClanRepository implements IClanRepository {

    private final Jdbi jdbi;
    private final ExecutorService executor;
    private final String serverId;

    /**
     * @param jdbi     the JDBI instance for database access
     * @param executor the shared database executor for async dispatch
     * @param serverId the server identifier used to scope all queries
     */
    public ClanRepository(Jdbi jdbi, ExecutorService executor, String serverId) {
        this.jdbi = jdbi;
        this.executor = executor;
        this.serverId = serverId;
    }

    @Override
    public CompletableFuture<Void> insertClan(ClanModel model) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.insertClan(
                                model.clanId().toString(),
                                serverId,
                                model.name(),
                                model.createdAt()
                        )), executor);
    }

    @Override
    public CompletableFuture<Void> deleteClan(UUID clanId) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.deleteClan(clanId.toString(), serverId)), executor);
    }

    @Override
    public CompletableFuture<Optional<ClanModel>> findClanById(UUID clanId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findClanById(clanId.toString(), serverId).map(this::toClanModel)), executor);
    }

    @Override
    public CompletableFuture<Optional<ClanModel>> findClanByName(String name) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findClanByName(serverId, name).map(this::toClanModel)), executor);
    }

    @Override
    public CompletableFuture<List<ClanModel>> findAllClans() {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findAllClans(serverId).stream().map(this::toClanModel).toList()), executor);
    }

    @Override
    public CompletableFuture<Void> updateMotd(UUID clanId, String motd) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.updateMotd(clanId.toString(), serverId, motd)), executor);
    }

    @Override
    public CompletableFuture<Void> insertMember(ClanMemberModel model) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.insertMember(
                                model.playerUuid().toString(),
                                serverId,
                                model.clanId().toString(),
                                model.isLeader(),
                                model.joinedAt()
                        )), executor);
    }

    @Override
    public CompletableFuture<Void> deleteMember(UUID playerUuid) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.deleteMember(playerUuid.toString(), serverId)), executor);
    }

    @Override
    public CompletableFuture<Optional<ClanMemberModel>> findMemberByPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findMemberByPlayer(playerUuid.toString(), serverId).map(this::toMemberModel)), executor);
    }

    @Override
    public CompletableFuture<List<ClanMemberModel>> findMembersByClan(UUID clanId) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findMembersByClan(clanId.toString(), serverId).stream()
                                .map(this::toMemberModel).toList()), executor);
    }

    @Override
    public CompletableFuture<Void> setLeader(UUID playerUuid, boolean isLeader) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.setLeader(playerUuid.toString(), serverId, isLeader)), executor);
    }

    @Override
    public CompletableFuture<Void> transferLeader(UUID oldLeaderUuid, UUID newLeaderUuid) {
        return CompletableFuture.runAsync(() ->
                jdbi.useTransaction(handle -> {
                    handle.createUpdate("UPDATE clan_members SET is_leader = false WHERE player_uuid = :uuid AND server_id = :serverId")
                            .bind("uuid", oldLeaderUuid.toString()).bind("serverId", serverId).execute();
                    handle.createUpdate("UPDATE clan_members SET is_leader = true WHERE player_uuid = :uuid AND server_id = :serverId")
                            .bind("uuid", newLeaderUuid.toString()).bind("serverId", serverId).execute();
                }), executor);
    }

    @Override
    public CompletableFuture<Void> insertPermission(UUID playerUuid, ClanPermission permission) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.insertPermission(playerUuid.toString(), serverId, permission.name())), executor);
    }

    @Override
    public CompletableFuture<Void> deletePermission(UUID playerUuid, ClanPermission permission) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(ClanDao.class, dao ->
                        dao.deletePermission(playerUuid.toString(), serverId, permission.name())), executor);
    }

    @Override
    public CompletableFuture<List<ClanPermission>> findPermissionsForPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(ClanDao.class, dao ->
                        dao.findPermissionsForPlayer(playerUuid.toString(), serverId).stream()
                                .map(ClanPermission::parse)
                                .flatMap(Optional::stream)
                                .toList()), executor);
    }

    private ClanModel toClanModel(ClanDao.ClanRow row) {
        return new ClanModel(
                UUID.fromString(row.clanId()),
                row.name(),
                row.motd(),
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
