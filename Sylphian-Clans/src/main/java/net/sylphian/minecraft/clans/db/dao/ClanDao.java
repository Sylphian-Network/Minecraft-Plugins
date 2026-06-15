package net.sylphian.minecraft.clans.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMappers;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * JDBI DAO for the {@code clans}, {@code clan_members}, and
 * {@code clan_member_permissions} tables.
 */
@RegisterConstructorMappers({
        @RegisterConstructorMapper(ClanDao.ClanRow.class),
        @RegisterConstructorMapper(ClanDao.MemberRow.class)
})
public interface ClanDao {

    /**
     * Inserts a new clan row.
     *
     * @param clanId    the clan's UUID as a string
     * @param name      the clan's display name
     * @param tag       the clan's short tag
     * @param createdAt epoch-second creation timestamp
     */
    @SqlUpdate("INSERT INTO clans (clan_id, name, tag, created_at) VALUES (:clanId, :name, :tag, :createdAt)")
    void insertClan(
            @Bind("clanId") String clanId,
            @Bind("name") String name,
            @Bind("tag") String tag,
            @Bind("createdAt") long createdAt
    );

    /**
     * Deletes a clan by ID. {@code ON DELETE CASCADE} removes member rows automatically.
     *
     * @param clanId the clan's UUID as a string
     */
    @SqlUpdate("DELETE FROM clans WHERE clan_id = :clanId")
    void deleteClan(@Bind("clanId") String clanId);

    /**
     * Finds a clan by its UUID.
     *
     * @param clanId the clan's UUID as a string
     * @return a row containing {@code clan_id, name, tag, created_at}, or empty
     */
    @SqlQuery("SELECT clan_id, name, tag, created_at FROM clans WHERE clan_id = :clanId")
    Optional<ClanRow> findClanById(@Bind("clanId") String clanId);

    /**
     * Finds a clan by its display name (case-sensitive).
     *
     * @param name the clan name to search for
     * @return a row, or empty if no clan has that name
     */
    @SqlQuery("SELECT clan_id, name, tag, created_at FROM clans WHERE name = :name")
    Optional<ClanRow> findClanByName(@Bind("name") String name);

    /**
     * Finds a clan by its tag (case-sensitive).
     *
     * @param tag the clan tag to search for
     * @return a row, or empty if no clan has that tag
     */
    @SqlQuery("SELECT clan_id, name, tag, created_at FROM clans WHERE tag = :tag")
    Optional<ClanRow> findClanByTag(@Bind("tag") String tag);

    /**
     * @return all clan rows ordered by name
     */
    @SqlQuery("SELECT clan_id, name, tag, created_at FROM clans ORDER BY name")
    List<ClanRow> findAllClans();

    /**
     * Inserts a new member row.
     *
     * @param playerUuid the member's UUID as a string
     * @param clanId     the clan's UUID as a string
     * @param isLeader   {@code true} if this member is the leader
     * @param joinedAt   epoch-second join timestamp
     */
    @SqlUpdate("INSERT INTO clan_members (player_uuid, clan_id, is_leader, joined_at) VALUES (:playerUuid, :clanId, :isLeader, :joinedAt)")
    void insertMember(
            @Bind("playerUuid") String playerUuid,
            @Bind("clanId") String clanId,
            @Bind("isLeader") boolean isLeader,
            @Bind("joinedAt") long joinedAt
    );

    /**
     * Deletes a member row by player UUID.
     *
     * @param playerUuid the member's UUID as a string
     */
    @SqlUpdate("DELETE FROM clan_members WHERE player_uuid = :playerUuid")
    void deleteMember(@Bind("playerUuid") String playerUuid);

    /**
     * Finds the member row for a given player.
     *
     * @param playerUuid the player's UUID as a string
     * @return the member row, or empty if the player is not in any clan
     */
    @SqlQuery("SELECT player_uuid, clan_id, is_leader, joined_at FROM clan_members WHERE player_uuid = :playerUuid")
    Optional<MemberRow> findMemberByPlayer(@Bind("playerUuid") String playerUuid);

    /**
     * Returns all member rows for the given clan.
     *
     * @param clanId the clan's UUID as a string
     * @return all member rows for this clan
     */
    @SqlQuery("SELECT player_uuid, clan_id, is_leader, joined_at FROM clan_members WHERE clan_id = :clanId")
    List<MemberRow> findMembersByClan(@Bind("clanId") String clanId);

    /**
     * Sets the {@code is_leader} flag for a member.
     *
     * @param playerUuid the member's UUID as a string
     * @param isLeader   the new leader flag value
     */
    @SqlUpdate("UPDATE clan_members SET is_leader = :isLeader WHERE player_uuid = :playerUuid")
    void setLeader(@Bind("playerUuid") String playerUuid, @Bind("isLeader") boolean isLeader);

    /**
     * Grants a permission to a member. Silently ignored if already present.
     *
     * @param playerUuid the member's UUID as a string
     * @param permission the {@link net.sylphian.minecraft.clans.model.ClanPermission} name
     */
    @SqlUpdate("INSERT IGNORE INTO clan_member_permissions (player_uuid, permission) VALUES (:playerUuid, :permission)")
    void insertPermission(@Bind("playerUuid") String playerUuid, @Bind("permission") String permission);

    /**
     * Revokes a specific permission from a member.
     *
     * @param playerUuid the member's UUID as a string
     * @param permission the {@link net.sylphian.minecraft.clans.model.ClanPermission} name
     */
    @SqlUpdate("DELETE FROM clan_member_permissions WHERE player_uuid = :playerUuid AND permission = :permission")
    void deletePermission(@Bind("playerUuid") String playerUuid, @Bind("permission") String permission);

    /**
     * Deletes all permission rows for a given player. Called when a member is
     * kicked or leaves before their member row is removed.
     *
     * @param playerUuid the member's UUID as a string
     */
    @SqlUpdate("DELETE FROM clan_member_permissions WHERE player_uuid = :playerUuid")
    void deleteAllPermissionsForPlayer(@Bind("playerUuid") String playerUuid);

    /**
     * Returns all permission names currently held by a player.
     *
     * @param playerUuid the player's UUID as a string
     * @return list of permission name strings
     */
    @SqlQuery("SELECT permission FROM clan_member_permissions WHERE player_uuid = :playerUuid")
    List<String> findPermissionsForPlayer(@Bind("playerUuid") String playerUuid);

    /** Raw row from the {@code clans} table. */
    record ClanRow(String clanId, String name, String tag, long createdAt) {}

    /** Raw row from the {@code clan_members} table. */
    record MemberRow(String playerUuid, String clanId, boolean isLeader, long joinedAt) {}
}
