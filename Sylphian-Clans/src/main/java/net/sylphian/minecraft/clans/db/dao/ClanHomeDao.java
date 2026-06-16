package net.sylphian.minecraft.clans.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

/**
 * JDBI DAO for the {@code clan_homes} table.
 */
@RegisterConstructorMapper(ClanHomeDao.HomeRow.class)
public interface ClanHomeDao {

    /**
     * Inserts or replaces the home for a clan.
     *
     * @param clanId the clan's UUID as a string
     * @param world  the world name
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param z      Z coordinate
     * @param yaw    yaw angle
     * @param pitch  pitch angle
     */
    @SqlUpdate("INSERT INTO clan_homes (clan_id, world, x, y, z, yaw, pitch) " +
               "VALUES (:clanId, :world, :x, :y, :z, :yaw, :pitch) " +
               "ON DUPLICATE KEY UPDATE world = :world, x = :x, y = :y, z = :z, yaw = :yaw, pitch = :pitch")
    void upsertHome(
            @Bind("clanId")  String clanId,
            @Bind("world")   String world,
            @Bind("x")       double x,
            @Bind("y")       double y,
            @Bind("z")       double z,
            @Bind("yaw")     float yaw,
            @Bind("pitch")   float pitch
    );

    /**
     * Returns the home for a clan, if one has been set.
     *
     * @param clanId the clan's UUID as a string
     * @return the home row, or empty if no home is set
     */
    @SqlQuery("SELECT clan_id, world, x, y, z, yaw, pitch FROM clan_homes WHERE clan_id = :clanId")
    Optional<HomeRow> findHomeByClan(@Bind("clanId") String clanId);

    /**
     * Deletes the home for a clan. No-op if no home is set.
     *
     * @param clanId the clan's UUID as a string
     */
    @SqlUpdate("DELETE FROM clan_homes WHERE clan_id = :clanId")
    void deleteHome(@Bind("clanId") String clanId);

    /** Raw row from the {@code clan_homes} table. */
    record HomeRow(String clanId, String world, double x, double y, double z, float yaw, float pitch) {}
}
