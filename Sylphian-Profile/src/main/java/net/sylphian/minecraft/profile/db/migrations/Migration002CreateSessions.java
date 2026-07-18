package net.sylphian.minecraft.profile.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/**
 * Migration V2: Creates the sylphian_profile_sessions table.
 * This table tracks individual player login sessions, linked to the sylphian_profile_players table via UUID.
 */
public class Migration002CreateSessions implements Migration {
    @Override
    public int version() { return 2; }

    @Override
    public String name() { return "CreateProfileSessions"; }

    @Override
    public String description() {
        return "Create sylphian_profile_sessions table";
    }

    @Override
    public void up(Handle handle) {
        // Create the sessions table with a foreign key back to the players table
        handle.execute("""
            CREATE TABLE sylphian_profile_sessions (
                session_id INT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                joined_at BIGINT NOT NULL,
                quit_at BIGINT NULL,
                duration BIGINT NOT NULL DEFAULT 0,
                FOREIGN KEY (player_uuid) REFERENCES sylphian_profile_players(player_uuid)
            )
        """);
    }

    @Override
    public void down(Handle handle) {
        // Drop the table to revert the migration
        handle.execute("DROP TABLE IF EXISTS sylphian_profile_sessions");
    }
}
