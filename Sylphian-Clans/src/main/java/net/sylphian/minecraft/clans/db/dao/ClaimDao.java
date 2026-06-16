package net.sylphian.minecraft.clans.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * JDBI DAO for the {@code clan_claims} table.
 */
@RegisterConstructorMapper(ClaimDao.ClaimRow.class)
public interface ClaimDao {

    /**
     * Inserts a new territory claim.
     *
     * @param serverId  the server this claim belongs to
     * @param world     the world name
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @param clanId    the owning clan's UUID as a string
     * @param claimedAt epoch-second claim timestamp
     */
    @SqlUpdate("INSERT INTO clan_claims (server_id, world, chunk_x, chunk_z, clan_id, claimed_at) VALUES (:serverId, :world, :chunkX, :chunkZ, :clanId, :claimedAt)")
    void insertClaim(
            @Bind("serverId") String serverId,
            @Bind("world") String world,
            @Bind("chunkX") int chunkX,
            @Bind("chunkZ") int chunkZ,
            @Bind("clanId") String clanId,
            @Bind("claimedAt") long claimedAt
    );

    /**
     * Deletes the claim for a specific chunk on a given server.
     *
     * @param serverId the server this claim belongs to
     * @param world    the world name
     * @param chunkX   the chunk X coordinate
     * @param chunkZ   the chunk Z coordinate
     */
    @SqlUpdate("DELETE FROM clan_claims WHERE server_id = :serverId AND world = :world AND chunk_x = :chunkX AND chunk_z = :chunkZ")
    void deleteClaim(
            @Bind("serverId") String serverId,
            @Bind("world") String world,
            @Bind("chunkX") int chunkX,
            @Bind("chunkZ") int chunkZ
    );

    /**
     * Deletes all claims owned by a clan on a given server. Used when a clan is disbanded.
     *
     * @param clanId   the owning clan's UUID as a string
     * @param serverId the server this clan belongs to
     */
    @SqlUpdate("DELETE FROM clan_claims WHERE clan_id = :clanId AND server_id = :serverId")
    void deleteAllClaimsForClan(@Bind("clanId") String clanId, @Bind("serverId") String serverId);

    /**
     * Finds the claim for a specific chunk on a given server.
     *
     * @param serverId the server to look up the claim on
     * @param world    the world name
     * @param chunkX   the chunk X coordinate
     * @param chunkZ   the chunk Z coordinate
     * @return the claim row, or empty if the chunk is unclaimed on this server
     */
    @SqlQuery("SELECT server_id, world, chunk_x, chunk_z, clan_id, claimed_at FROM clan_claims WHERE server_id = :serverId AND world = :world AND chunk_x = :chunkX AND chunk_z = :chunkZ")
    Optional<ClaimRow> findClaimByChunk(
            @Bind("serverId") String serverId,
            @Bind("world") String world,
            @Bind("chunkX") int chunkX,
            @Bind("chunkZ") int chunkZ
    );

    /**
     * Returns all claims owned by the given clan on a given server.
     *
     * @param clanId   the owning clan's UUID as a string
     * @param serverId the server this clan belongs to
     * @return all claim rows for this clan
     */
    @SqlQuery("SELECT server_id, world, chunk_x, chunk_z, clan_id, claimed_at FROM clan_claims WHERE clan_id = :clanId AND server_id = :serverId")
    List<ClaimRow> findClaimsByClan(@Bind("clanId") String clanId, @Bind("serverId") String serverId);

    /**
     * Returns all claim rows for a given server. Used to seed the {@code TerritoryCache} on startup.
     *
     * @param serverId the server to load claims for
     * @return all claim rows for this server
     */
    @SqlQuery("SELECT server_id, world, chunk_x, chunk_z, clan_id, claimed_at FROM clan_claims WHERE server_id = :serverId")
    List<ClaimRow> findAllClaims(@Bind("serverId") String serverId);

    /** Raw row from the {@code clan_claims} table. */
    record ClaimRow(String serverId, String world, int chunkX, int chunkZ, String clanId, long claimedAt) {}
}
