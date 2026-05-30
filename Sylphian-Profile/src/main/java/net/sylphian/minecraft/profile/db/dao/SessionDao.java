package net.sylphian.minecraft.profile.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * JDBI DAO for the mc_sessions table.
 * Handles persistence for player session login/logout data.
 */
@RegisterConstructorMapper(SessionDao.SessionRow.class)
public interface SessionDao {

    /**
     * Finds a session row by its unique ID.
     * @param sessionId the session ID
     * @return an optional session row
     */
    @SqlQuery("SELECT * FROM mc_sessions WHERE session_id = :sessionId")
    Optional<SessionRow> findById(@Bind("sessionId") int sessionId);

    /**
     * Finds all session rows for a player.
     * @param uuid the player's UUID as a string
     * @return a list of session rows
     */
    @SqlQuery("SELECT * FROM mc_sessions WHERE uuid = :uuid")
    List<SessionRow> findByUuid(@Bind("uuid") String uuid);

    /**
     * Finds the currently open session for a player.
     * @param uuid the player's UUID as a string
     * @return an optional session row
     */
    @SqlQuery("SELECT * FROM mc_sessions WHERE uuid = :uuid AND quit_at IS NULL LIMIT 1")
    Optional<SessionRow> findOpenByUuid(@Bind("uuid") String uuid);

    /**
     * Inserts a complete session record.
     *
     * @param uuid     player UUID
     * @param joinedAt join timestamp
     * @param quitAt   quit timestamp
     * @param duration session duration
     * @return the generated session ID
     */
    @SqlUpdate("INSERT INTO mc_sessions (uuid, joined_at, quit_at, duration) " +
            "VALUES (:uuid, :joinedAt, :quitAt, :duration)")
    @GetGeneratedKeys
    int insert(@Bind("uuid") String uuid, @Bind("joinedAt") long joinedAt,
               @Bind("quitAt") Long quitAt, @Bind("duration") long duration);

    /**
     * Updates an existing session record.
     * @param sessionId the session ID
     * @param quitAt    quit timestamp
     * @param duration  session duration
     */
    @SqlUpdate("UPDATE mc_sessions SET quit_at = :quitAt, duration = :duration " +
            "WHERE session_id = :sessionId")
    void update(@Bind("sessionId") int sessionId, @Bind("quitAt") Long quitAt,
                @Bind("duration") long duration);

    /**
     * Opens a new session for a player.
     * @param uuid     player UUID
     * @param joinedAt join timestamp
     * @return the generated session ID
     */
    @SqlUpdate("INSERT INTO mc_sessions (uuid, joined_at, duration) VALUES (:uuid, :joinedAt, 0)")
    @GetGeneratedKeys
    int open(@Bind("uuid") String uuid, @Bind("joinedAt") long joinedAt);

    /**
     * Closes an active session.
     * @param sessionId the session ID
     * @param quitAt    quit timestamp
     * @param duration  total duration
     */
    @SqlUpdate("UPDATE mc_sessions SET quit_at = :quitAt, duration = :duration WHERE session_id = :sessionId")
    void close(@Bind("sessionId") int sessionId, @Bind("quitAt") long quitAt, @Bind("duration") long duration);

    /**
     * Internal data transfer object for JDBI mapping.
     */
    record SessionRow(int sessionId, String uuid, long joinedAt, Long quitAt, long duration) {}
}