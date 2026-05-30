package net.sylphian.minecraft.fishing.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/**
 * Migration version 1: CreateFishEncyclopaedia.
 * Creates the fish_encyclopaedia table to track player fish catches.
 */
public class Migration001CreateFishEncyclopaedia implements Migration {

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String name() {
        return "CreateFishEncyclopaedia";
    }

    @Override
    public String description() {
        return "Create fish_encyclopaedia table";
    }

    @Override
    public void up(Handle handle) {
        // Create the fish_encyclopaedia table with player UUID and fish ID as primary key
        // Stores total catches, max weight caught, and first/last catch timestamps
        handle.execute("""
            CREATE TABLE IF NOT EXISTS fish_encyclopaedia (
                uuid VARCHAR(36) NOT NULL,
                fish_id VARCHAR(64) NOT NULL,
                times_caught INT NOT NULL DEFAULT 1,
                biggest_weight DOUBLE NOT NULL,
                first_caught BIGINT NOT NULL,
                last_caught BIGINT NOT NULL,
                PRIMARY KEY (uuid, fish_id),
                FOREIGN KEY (uuid) REFERENCES mc_players(uuid)
            )
        """);
    }

    @Override
    public void down(Handle handle) {
        // Drop the fish_encyclopaedia table to undo the migration
        handle.execute("DROP TABLE IF EXISTS fish_encyclopaedia");
    }
}
