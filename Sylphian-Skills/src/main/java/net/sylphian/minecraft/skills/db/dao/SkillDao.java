package net.sylphian.minecraft.skills.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * JDBI DAO for the player_skills table.
 */
@RegisterConstructorMapper(SkillDao.SkillRow.class)
public interface SkillDao {

    /**
     * Loads all skill XP rows for a player.
     *
     * @param uuid the player's UUID string
     * @return all skill rows for this player
     */
    @SqlQuery("SELECT skill_id, xp FROM player_skills WHERE player_uuid = :uuid")
    List<SkillRow> findAll(@Bind("uuid") String uuid);

    /**
     * Loads the XP for a single skill, or {@code null} if no row exists yet.
     *
     * @param uuid    the player's UUID string
     * @param skillId the skill identifier
     * @return the stored XP, or {@code null} if not found
     */
    @SqlQuery("SELECT xp FROM player_skills WHERE player_uuid = :uuid AND skill_id = :skillId")
    Long findOne(@Bind("uuid") String uuid, @Bind("skillId") String skillId);

    /**
     * Inserts or updates a player's XP for a skill, keeping whichever value is higher.
     * Using GREATEST guards against out-of-order async writes overwriting a newer value.
     *
     * @param uuid    the player's UUID string
     * @param skillId the skill identifier
     * @param xp      the new total XP value
     */
    @SqlUpdate("""
        INSERT INTO player_skills (player_uuid, skill_id, xp)
        VALUES (:uuid, :skillId, :xp)
        ON DUPLICATE KEY UPDATE xp = GREATEST(xp, VALUES(xp))
    """)
    void upsertXP(@Bind("uuid") String uuid, @Bind("skillId") String skillId, @Bind("xp") long xp);

    /**
     * Internal data transfer object for JDBI mapping.
     *
     * @param skillId the skill identifier
     * @param xp      total accumulated XP
     */
    record SkillRow(String skillId, long xp) {}
}
