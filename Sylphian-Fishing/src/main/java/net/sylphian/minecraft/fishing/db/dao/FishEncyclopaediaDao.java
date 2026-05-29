package net.sylphian.minecraft.fishing.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(FishEncyclopaediaDao.FishEncyclopaediaRow.class)
public interface FishEncyclopaediaDao {

    @SqlQuery("SELECT * FROM fish_encyclopaedia WHERE uuid = :uuid AND fish_id = :fishId")
    Optional<FishEncyclopaediaRow> findEntry(@Bind("uuid") String uuid,
                                             @Bind("fishId") String fishId);

    @SqlQuery("SELECT * FROM fish_encyclopaedia WHERE uuid = :uuid")
    List<FishEncyclopaediaRow> findAllForPlayer(@Bind("uuid") String uuid);

    @SqlUpdate("""
            INSERT INTO fish_encyclopaedia (uuid, fish_id, times_caught, biggest_weight, first_caught, last_caught)
            VALUES (:uuid, :fishId, 1, :weight, :now, :now)
            """)
    void insert(@Bind("uuid") String uuid, @Bind("fishId") String fishId,
                @Bind("weight") double weight, @Bind("now") long now);

    @SqlUpdate("""
            UPDATE fish_encyclopaedia
            SET times_caught = times_caught + 1,
                biggest_weight = CASE WHEN :weight > biggest_weight THEN :weight ELSE biggest_weight END,
                last_caught = :now
            WHERE uuid = :uuid AND fish_id = :fishId
            """)
    void update(@Bind("uuid") String uuid, @Bind("fishId") String fishId,
                @Bind("weight") double weight, @Bind("now") long now);

    record FishEncyclopaediaRow(String uuid, String fishId, int timesCaught,
                                double biggestWeight, long firstCaught, long lastCaught) {}
}