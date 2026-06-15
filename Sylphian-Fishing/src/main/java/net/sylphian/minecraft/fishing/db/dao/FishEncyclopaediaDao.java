package net.sylphian.minecraft.fishing.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the fish_encyclopaedia table.
 * Uses JDBI SQL Objects to map SQL queries to Java methods.
 */
@RegisterConstructorMapper(FishEncyclopaediaDao.FishEncyclopaediaRow.class)
public interface FishEncyclopaediaDao {

    /** Finds a single entry for a player and fish ID. */
    @SqlQuery("SELECT * FROM fish_encyclopaedia WHERE uuid = :uuid AND fish_id = :fishId")
    Optional<FishEncyclopaediaRow> findEntry(@Bind("uuid") String uuid, @Bind("fishId") String fishId);

    /** Retrieves all fish catch records for a given player. */
    @SqlQuery("SELECT * FROM fish_encyclopaedia WHERE uuid = :uuid")
    List<FishEncyclopaediaRow> findAllForPlayer(@Bind("uuid") String uuid);

    /** Inserts a new catch record for a player. */
    @SqlUpdate("""
            INSERT INTO fish_encyclopaedia (uuid, fish_id, times_caught, biggest_weight, first_caught, last_caught)
            VALUES (:uuid, :fishId, 1, :weight, :now, :now)
            """)
    void insert(@Bind("uuid") String uuid, @Bind("fishId") String fishId, @Bind("weight") double weight, @Bind("now") long now);

    /** Updates an existing catch record, incrementing count and updating the biggest weight if exceeded. */
    @SqlUpdate("""
            UPDATE fish_encyclopaedia
            SET times_caught = times_caught + 1,
                biggest_weight = CASE WHEN :weight > biggest_weight THEN :weight ELSE biggest_weight END,
                last_caught = :now
            WHERE uuid = :uuid AND fish_id = :fishId
            """)
    void update(@Bind("uuid") String uuid, @Bind("fishId") String fishId, @Bind("weight") double weight, @Bind("now") long now);

    /**
     * Atomically inserts or updates a catch record.
     * On duplicate key, increments times_caught, updates biggest_weight if exceeded, and refreshes last_caught.
     * first_caught is never overwritten.
     */
    @SqlUpdate("""
            INSERT INTO fish_encyclopaedia (uuid, fish_id, times_caught, biggest_weight, first_caught, last_caught)
            VALUES (:uuid, :fishId, 1, :weight, :now, :now)
            ON DUPLICATE KEY UPDATE
                times_caught   = times_caught + 1,
                biggest_weight = CASE WHEN :weight > biggest_weight THEN :weight ELSE biggest_weight END,
                last_caught    = :now
            """)
    void upsert(@Bind("uuid") String uuid, @Bind("fishId") String fishId, @Bind("weight") double weight, @Bind("now") long now);

    /** Represents a raw row from the fish_encyclopaedia table. */
    record FishEncyclopaediaRow(String uuid, String fishId, int timesCaught, double biggestWeight, long firstCaught, long lastCaught) {}
}