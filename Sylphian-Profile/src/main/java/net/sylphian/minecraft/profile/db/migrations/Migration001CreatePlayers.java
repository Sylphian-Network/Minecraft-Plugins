package net.sylphian.minecraft.profile.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/**
 * Migration V1: Creates the sylphian_profile_players table.
 * This table stores persistent player data, including XenForo link info and total playtime.
 */
public class Migration001CreatePlayers implements Migration {
    @Override
    public int version() { return 1; }

    @Override
    public String name() { return "CreateProfilePlayers"; }

    @Override
    public String description() {
        return "Create sylphian_profile_players table";
    }

    @Override
    public void up(Handle handle) {
        // Create the primary players table with UUID as the primary key
        handle.execute("""
            CREATE TABLE sylphian_profile_players (
                player_uuid VARCHAR(36) PRIMARY KEY,
                xf_user_id INT NULL,
                mc_username VARCHAR(16) NOT NULL,
                forum_username VARCHAR(255) NULL,
                first_joined BIGINT NOT NULL,
                last_seen BIGINT NOT NULL,
                playtime BIGINT NOT NULL DEFAULT 0,
                is_online BOOLEAN NOT NULL DEFAULT FALSE
            )
        """);
    }

    @Override
    public void down(Handle handle) {
        // Drop the table to revert the migration
        handle.execute("DROP TABLE IF EXISTS sylphian_profile_players");
    }
}
