package net.sylphian.minecraft.profile.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(SessionDao.SessionRow.class)
public interface SessionDao {

    @SqlQuery("SELECT * FROM mc_sessions WHERE session_id = :sessionId")
    Optional<SessionRow> findById(@Bind("sessionId") int sessionId);

    @SqlQuery("SELECT * FROM mc_sessions WHERE uuid = :uuid")
    List<SessionRow> findByUuid(@Bind("uuid") String uuid);

    @SqlQuery("SELECT * FROM mc_sessions WHERE uuid = :uuid AND quit_at IS NULL LIMIT 1")
    Optional<SessionRow> findOpenByUuid(@Bind("uuid") String uuid);

    @SqlUpdate("INSERT INTO mc_sessions (uuid, joined_at, quit_at, duration) " +
            "VALUES (:uuid, :joinedAt, :quitAt, :duration)")
    @GetGeneratedKeys
    int insert(@Bind("uuid") String uuid, @Bind("joinedAt") long joinedAt,
               @Bind("quitAt") Long quitAt, @Bind("duration") long duration);

    @SqlUpdate("UPDATE mc_sessions SET quit_at = :quitAt, duration = :duration " +
            "WHERE session_id = :sessionId")
    void update(@Bind("sessionId") int sessionId, @Bind("quitAt") Long quitAt,
                @Bind("duration") long duration);

    @SqlUpdate("INSERT INTO mc_sessions (uuid, joined_at, duration) VALUES (:uuid, :joinedAt, 0)")
    @GetGeneratedKeys
    int open(@Bind("uuid") String uuid, @Bind("joinedAt") long joinedAt);

    @SqlUpdate("UPDATE mc_sessions SET quit_at = :quitAt, duration = :duration WHERE session_id = :sessionId")
    void close(@Bind("sessionId") int sessionId, @Bind("quitAt") long quitAt, @Bind("duration") long duration);

    record SessionRow(int sessionId, String uuid, long joinedAt, Long quitAt, long duration) {}
}