package net.sylphian.minecraft.profile.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

/**
 * JDBI DAO for the mc_players table.
 * Handles low-level SQL operations for player records.
 */
@RegisterConstructorMapper(PlayerDao.PlayerRow.class)
public interface PlayerDao {

    /**
     * Finds a player row by UUID.
     * @param uuid the player's UUID as a string
     * @return an optional player row
     */
    @SqlQuery("SELECT * FROM mc_players WHERE uuid = :uuid")
    Optional<PlayerRow> findByUuid(@Bind("uuid") String uuid);

    /**
     * Finds a player row by XenForo user ID.
     * @param xfUserId the XenForo user ID
     * @return an optional player row
     */
    @SqlQuery("SELECT * FROM mc_players WHERE xf_user_id = :xfUserId")
    Optional<PlayerRow> findByXfUserId(@Bind("xfUserId") Integer xfUserId);

    /**
     * Inserts a new player record.
     *
     * @param uuid          player UUID
     * @param xfUserId      linked forum ID
     * @param mcUsername    minecraft name
     * @param forumUsername forum name
     * @param firstJoined   join timestamp
     * @param lastSeen      last seen timestamp
     * @param playtime      cumulative playtime
     * @param isOnline      online status flag
     */
    @SqlUpdate("INSERT INTO mc_players (uuid, xf_user_id, mc_username, forum_username, first_joined, last_seen, playtime, is_online) " +
            "VALUES (:uuid, :xfUserId, :mcUsername, :forumUsername, :firstJoined, :lastSeen, :playtime, :isOnline)")
    void insert(@Bind("uuid") String uuid, @Bind("xfUserId") Integer xfUserId,
                @Bind("mcUsername") String mcUsername, @Bind("forumUsername") String forumUsername,
                @Bind("firstJoined") long firstJoined, @Bind("lastSeen") long lastSeen,
                @Bind("playtime") long playtime, @Bind("isOnline") boolean isOnline);

    /**
     * Updates an existing player record.
     *
     * @param uuid          player UUID (key)
     * @param xfUserId      linked forum ID
     * @param mcUsername    minecraft name
     * @param forumUsername forum name
     * @param lastSeen      last seen timestamp
     * @param playtime      cumulative playtime
     * @param isOnline      online status flag
     */
    @SqlUpdate("UPDATE mc_players SET xf_user_id = :xfUserId, mc_username = :mcUsername, " +
            "forum_username = :forumUsername, last_seen = :lastSeen, playtime = :playtime, " +
            "is_online = :isOnline WHERE uuid = :uuid")
    void update(@Bind("uuid") String uuid, @Bind("xfUserId") Integer xfUserId,
                @Bind("mcUsername") String mcUsername, @Bind("forumUsername") String forumUsername,
                @Bind("lastSeen") long lastSeen, @Bind("playtime") long playtime,
                @Bind("isOnline") boolean isOnline);

    /**
     * Internal data transfer object for JDBI mapping.
     */
    record PlayerRow(String uuid, Integer xfUserId, String mcUsername,
                     String forumUsername, long firstJoined, long lastSeen,
                     long playtime, boolean isOnline) {}
}