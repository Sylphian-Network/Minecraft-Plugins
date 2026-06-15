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
     * @param world     the world name
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @param clanId    the owning clan's UUID as a string
     * @param claimedAt epoch-second claim timestamp
     */
    @SqlUpdate("INSERT INTO clan_claims (world, chunk_x, chunk_z, clan_id, claimed_at) VALUES (:world, :chunkX, :chunkZ, :clanId, :claimedAt)")
    void insertClaim(
            @Bind("world") String world,
            @Bind("chunkX") int chunkX,
            @Bind("chunkZ") int chunkZ,
            @Bind("clanId") String clanId,
            @Bind("claimedAt") long claimedAt
    );

    /**
     * Deletes the claim for a specific chunk.
     *
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     */
    @SqlUpdate("DELETE FROM clan_claims WHERE world = :world AND chunk_x = :chunkX AND chunk_z = :chunkZ")
    void deleteClaim(
            @Bind("world") String world,
            @Bind("chunkX") int chunkX,
            @Bind("chunkZ") int chunkZ
    );

    /**
     * Deletes all claims owned by a clan. Used when a clan is disbanded.
     *
     * @param clanId the owning clan's UUID as a string
     */
    @SqlUpdate("DELETE FROM clan_claims WHERE clan_id = :clanId")
    void deleteAllClaimsForClan(@Bind("clanId") String clanId);

    /**
     * Finds the claim for a specific chunk.
     *
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the claim row, or empty if the chunk is unclaimed
     */
    @SqlQuery("SELECT world, chunk_x, chunk_z, clan_id, claimed_at FROM clan_claims WHERE world = :world AND chunk_x = :chunkX AND chunk_z = :chunkZ")
    Optional<ClaimRow> findClaimByChunk(
            @Bind("world") String world,
            @Bind("chunkX") int chunkX,
            @Bind("chunkZ") int chunkZ
    );

    /**
     * Returns all claims owned by the given clan.
     *
     * @param clanId the owning clan's UUID as a string
     * @return all claim rows for this clan
     */
    @SqlQuery("SELECT world, chunk_x, chunk_z, clan_id, claimed_at FROM clan_claims WHERE clan_id = :clanId")
    List<ClaimRow> findClaimsByClan(@Bind("clanId") String clanId);

    /**
     * @return all claim rows across all clans, used to seed the {@code TerritoryCache} on startup
     */
    @SqlQuery("SELECT world, chunk_x, chunk_z, clan_id, claimed_at FROM clan_claims")
    List<ClaimRow> findAllClaims();

    /** Raw row from the {@code clan_claims} table. */
    record ClaimRow(String world, int chunkX, int chunkZ, String clanId, long claimedAt) {}
}
