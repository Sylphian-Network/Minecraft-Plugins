package net.sylphian.minecraft.cooking.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the cooking_mastery table.
 * Uses JDBI SQL Objects to map SQL queries to Java methods.
 */
@RegisterConstructorMapper(CookingMasteryDao.MasteryRow.class)
public interface CookingMasteryDao {

    /** Finds the mastery record for a player and recipe. */
    @SqlQuery("SELECT player_uuid, recipe_id, cook_count FROM cooking_mastery WHERE player_uuid = :uuid AND recipe_id = :recipeId")
    Optional<MasteryRow> findEntry(@Bind("uuid") String uuid, @Bind("recipeId") String recipeId);

    /** Retrieves all mastery records for a player. */
    @SqlQuery("SELECT player_uuid, recipe_id, cook_count FROM cooking_mastery WHERE player_uuid = :uuid")
    List<MasteryRow> findAllForPlayer(@Bind("uuid") String uuid);

    /** Inserts a new mastery record with cook_count 1, or increments the existing count by one. */
    @SqlUpdate("""
        INSERT INTO cooking_mastery (player_uuid, recipe_id, cook_count)
        VALUES (:uuid, :recipeId, 1)
        ON DUPLICATE KEY UPDATE cook_count = cook_count + 1
    """)
    void incrementCount(@Bind("uuid") String uuid, @Bind("recipeId") String recipeId);

    /** Represents a raw row from the cooking_mastery table. */
    record MasteryRow(String playerUuid, String recipeId, int cookCount) {}
}
