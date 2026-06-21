package net.sylphian.minecraft.clans.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * JDBI DAO for the {@code clan_warps} and {@code clan_warp_access} tables.
 */
@RegisterConstructorMapper(ClanWarpDao.WarpRow.class)
public interface ClanWarpDao {

    /**
     * Inserts a warp, or updates its location, icon, and description if the name already
     * exists for the clan. Never changes the {@code restricted} flag; new warps default to public.
     */
    @SqlUpdate("INSERT INTO clan_warps (clan_id, name, world, x, y, z, yaw, pitch, icon, description, restricted) " +
               "VALUES (:clanId, :name, :world, :x, :y, :z, :yaw, :pitch, :icon, :description, 0) " +
               "ON DUPLICATE KEY UPDATE world = :world, x = :x, y = :y, z = :z, yaw = :yaw, pitch = :pitch, " +
               "icon = :icon, description = :description")
    void upsertWarp(
            @Bind("clanId")      String clanId,
            @Bind("name")        String name,
            @Bind("world")       String world,
            @Bind("x")           double x,
            @Bind("y")           double y,
            @Bind("z")           double z,
            @Bind("yaw")         float yaw,
            @Bind("pitch")       float pitch,
            @Bind("icon")        String icon,
            @Bind("description") String description
    );

    /** @return the warp, or empty if no warp with that name exists for the clan */
    @SqlQuery("SELECT clan_id, name, world, x, y, z, yaw, pitch, icon, description, restricted " +
              "FROM clan_warps WHERE clan_id = :clanId AND name = :name")
    Optional<WarpRow> findWarp(@Bind("clanId") String clanId, @Bind("name") String name);

    /** @return every warp owned by the clan, ordered by name */
    @SqlQuery("SELECT clan_id, name, world, x, y, z, yaw, pitch, icon, description, restricted " +
              "FROM clan_warps WHERE clan_id = :clanId ORDER BY name")
    List<WarpRow> findWarpsByClan(@Bind("clanId") String clanId);

    /** @return the number of warps the clan owns */
    @SqlQuery("SELECT COUNT(*) FROM clan_warps WHERE clan_id = :clanId")
    int countWarps(@Bind("clanId") String clanId);

    /** Deletes a warp. Access rows for it are removed by the foreign-key cascade. */
    @SqlUpdate("DELETE FROM clan_warps WHERE clan_id = :clanId AND name = :name")
    void deleteWarp(@Bind("clanId") String clanId, @Bind("name") String name);

    /** Sets whether a warp is restricted to its access list. */
    @SqlUpdate("UPDATE clan_warps SET restricted = :restricted WHERE clan_id = :clanId AND name = :name")
    void setRestricted(@Bind("clanId") String clanId, @Bind("name") String name, @Bind("restricted") boolean restricted);

    /** Grants a member access to a warp. Silently ignored if already granted. */
    @SqlUpdate("INSERT IGNORE INTO clan_warp_access (clan_id, warp_name, player_uuid) " +
               "VALUES (:clanId, :warpName, :playerUuid)")
    void insertAccess(@Bind("clanId") String clanId, @Bind("warpName") String warpName, @Bind("playerUuid") String playerUuid);

    /** Revokes a member's access to a warp. No-op if not granted. */
    @SqlUpdate("DELETE FROM clan_warp_access WHERE clan_id = :clanId AND warp_name = :warpName AND player_uuid = :playerUuid")
    void deleteAccess(@Bind("clanId") String clanId, @Bind("warpName") String warpName, @Bind("playerUuid") String playerUuid);

    /** @return {@code true} if the member has an access row for the warp */
    @SqlQuery("SELECT EXISTS(SELECT 1 FROM clan_warp_access WHERE clan_id = :clanId AND warp_name = :warpName AND player_uuid = :playerUuid)")
    boolean hasAccess(@Bind("clanId") String clanId, @Bind("warpName") String warpName, @Bind("playerUuid") String playerUuid);

    /** @return the UUID strings of all members granted access to the warp */
    @SqlQuery("SELECT player_uuid FROM clan_warp_access WHERE clan_id = :clanId AND warp_name = :warpName")
    List<String> findAccessByWarp(@Bind("clanId") String clanId, @Bind("warpName") String warpName);

    /** @return the names of all warps the given member has been granted access to */
    @SqlQuery("SELECT warp_name FROM clan_warp_access WHERE clan_id = :clanId AND player_uuid = :playerUuid")
    List<String> findAccessibleWarps(@Bind("clanId") String clanId, @Bind("playerUuid") String playerUuid);

    /** Raw row from the {@code clan_warps} table. */
    record WarpRow(String clanId, String name, String world, double x, double y, double z,
                   float yaw, float pitch, String icon, String description, boolean restricted) {}
}
