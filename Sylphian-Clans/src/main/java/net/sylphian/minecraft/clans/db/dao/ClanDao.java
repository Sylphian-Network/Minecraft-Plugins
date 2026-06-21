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
     * @param serverId  the server this clan belongs to
     * @param name      the clan's display name
     * @param createdAt epoch-second creation timestamp
     */
    @SqlUpdate("INSERT INTO clans (clan_id, server_id, name, created_at) VALUES (:clanId, :serverId, :name, :createdAt)")
    void insertClan(
            @Bind("clanId") String clanId,
            @Bind("serverId") String serverId,
            @Bind("name") String name,
            @Bind("createdAt") long createdAt
    );

    /**
     * Deletes a clan by ID and server. {@code ON DELETE CASCADE} removes member rows automatically.
     *
     * @param clanId   the clan's UUID as a string
     * @param serverId the server this clan belongs to
     */
    @SqlUpdate("DELETE FROM clans WHERE clan_id = :clanId AND server_id = :serverId")
    void deleteClan(@Bind("clanId") String clanId, @Bind("serverId") String serverId);

    /**
     * Finds a clan by its UUID on a given server.
     *
     * @param clanId   the clan's UUID as a string
     * @param serverId the server to look up the clan on
     * @return a row containing clan fields, or empty
     */
    @SqlQuery("SELECT clan_id, server_id, name, motd, created_at FROM clans WHERE clan_id = :clanId AND server_id = :serverId")
    Optional<ClanRow> findClanById(@Bind("clanId") String clanId, @Bind("serverId") String serverId);

    /**
     * Finds a clan by its display name within a server (case-sensitive).
     *
     * @param serverId the server to search within
     * @param name     the clan name to search for
     * @return a row, or empty if no clan has that name on this server
     */
    @SqlQuery("SELECT clan_id, server_id, name, motd, created_at FROM clans WHERE server_id = :serverId AND name = :name")
    Optional<ClanRow> findClanByName(@Bind("serverId") String serverId, @Bind("name") String name);

    /**
     * @param serverId the server to list clans for
     * @return all clan rows for this server, ordered by name
     */
    @SqlQuery("SELECT clan_id, server_id, name, motd, created_at FROM clans WHERE server_id = :serverId ORDER BY name")
    List<ClanRow> findAllClans(@Bind("serverId") String serverId);

    /**
     * Sets (or clears, when {@code motd} is null) the message of the day for a clan.
     *
     * @param clanId   the clan's UUID as a string
     * @param serverId the server this clan belongs to
     * @param motd     the new MOTD, or null to clear
     */
    @SqlUpdate("UPDATE clans SET motd = :motd WHERE clan_id = :clanId AND server_id = :serverId")
    void updateMotd(@Bind("clanId") String clanId, @Bind("serverId") String serverId, @Bind("motd") String motd);

    /**
     * Inserts a new member row.
     *
     * @param playerUuid the member's UUID as a string
     * @param serverId   the server this membership belongs to
     * @param clanId     the clan's UUID as a string
     * @param isLeader   {@code true} if this member is the leader
     * @param joinedAt   epoch-second join timestamp
     */
    @SqlUpdate("INSERT INTO clan_members (player_uuid, server_id, clan_id, is_leader, joined_at) VALUES (:playerUuid, :serverId, :clanId, :isLeader, :joinedAt)")
    void insertMember(
            @Bind("playerUuid") String playerUuid,
            @Bind("serverId") String serverId,
            @Bind("clanId") String clanId,
            @Bind("isLeader") boolean isLeader,
            @Bind("joinedAt") long joinedAt
    );

    /**
     * Deletes a member row by player UUID and server.
     *
     * @param playerUuid the member's UUID as a string
     * @param serverId   the server this membership belongs to
     */
    @SqlUpdate("DELETE FROM clan_members WHERE player_uuid = :playerUuid AND server_id = :serverId")
    void deleteMember(@Bind("playerUuid") String playerUuid, @Bind("serverId") String serverId);

    /**
     * Finds the member row for a given player on a given server.
     *
     * @param playerUuid the player's UUID as a string
     * @param serverId   the server to look up membership on
     * @return the member row, or empty if the player is not in any clan on this server
     */
    @SqlQuery("SELECT player_uuid, server_id, clan_id, is_leader, joined_at FROM clan_members WHERE player_uuid = :playerUuid AND server_id = :serverId")
    Optional<MemberRow> findMemberByPlayer(@Bind("playerUuid") String playerUuid, @Bind("serverId") String serverId);

    /**
     * Returns all member rows for the given clan on a given server.
     *
     * @param clanId   the clan's UUID as a string
     * @param serverId the server this clan belongs to
     * @return all member rows for this clan
     */
    @SqlQuery("SELECT player_uuid, server_id, clan_id, is_leader, joined_at FROM clan_members WHERE clan_id = :clanId AND server_id = :serverId")
    List<MemberRow> findMembersByClan(@Bind("clanId") String clanId, @Bind("serverId") String serverId);

    /**
     * Sets the {@code is_leader} flag for a member on a given server.
     *
     * @param playerUuid the member's UUID as a string
     * @param serverId   the server this membership belongs to
     * @param isLeader   the new leader flag value
     */
    @SqlUpdate("UPDATE clan_members SET is_leader = :isLeader WHERE player_uuid = :playerUuid AND server_id = :serverId")
    void setLeader(@Bind("playerUuid") String playerUuid, @Bind("serverId") String serverId, @Bind("isLeader") boolean isLeader);

    /**
     * Grants a permission to a member. Silently ignored if already present.
     *
     * @param playerUuid the member's UUID as a string
     * @param serverId   the server this permission applies to
     * @param permission the {@link net.sylphian.minecraft.clans.model.ClanPermission} name
     */
    @SqlUpdate("INSERT IGNORE INTO clan_member_permissions (player_uuid, server_id, permission) VALUES (:playerUuid, :serverId, :permission)")
    void insertPermission(@Bind("playerUuid") String playerUuid, @Bind("serverId") String serverId, @Bind("permission") String permission);

    /**
     * Revokes a specific permission from a member.
     *
     * @param playerUuid the member's UUID as a string
     * @param serverId   the server this permission applies to
     * @param permission the {@link net.sylphian.minecraft.clans.model.ClanPermission} name
     */
    @SqlUpdate("DELETE FROM clan_member_permissions WHERE player_uuid = :playerUuid AND server_id = :serverId AND permission = :permission")
    void deletePermission(@Bind("playerUuid") String playerUuid, @Bind("serverId") String serverId, @Bind("permission") String permission);

    /**
     * Returns all permission names currently held by a player on a given server.
     *
     * @param playerUuid the player's UUID as a string
     * @param serverId   the server to look up permissions for
     * @return list of permission name strings
     */
    @SqlQuery("SELECT permission FROM clan_member_permissions WHERE player_uuid = :playerUuid AND server_id = :serverId")
    List<String> findPermissionsForPlayer(@Bind("playerUuid") String playerUuid, @Bind("serverId") String serverId);

    /**
     * Returns one row per (member, permission) for a clan, with a null permission
     * for members who hold none (via LEFT JOIN). Used to load a clan in two queries.
     */
    @SqlQuery("""
        SELECT m.player_uuid AS playerUuid, p.permission AS permission
        FROM clan_members m
        LEFT JOIN clan_member_permissions p
               ON p.player_uuid = m.player_uuid AND p.server_id = m.server_id
        WHERE m.clan_id = :clanId AND m.server_id = :serverId
        """)
    @RegisterConstructorMapper(PermissionRow.class)
    List<PermissionRow> findPermissionsByClan(@Bind("clanId") String clanId, @Bind("serverId") String serverId);

    /** A member UUID paired with one of their permission names (null if they have none). */
    record PermissionRow(String playerUuid, String permission) {}

    /** Raw row from the {@code clans} table. */
    record ClanRow(String clanId, String serverId, String name, String motd, long createdAt) {}

    /** Raw row from the {@code clan_members} table. */
    record MemberRow(String playerUuid, String serverId, String clanId, boolean isLeader, long joinedAt) {}
}
