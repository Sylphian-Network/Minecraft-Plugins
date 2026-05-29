package net.sylphian.minecraft.fishing.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

public class Migration003CreateFishEncyclopaedia implements Migration {

    @Override
    public int version() { return 3; }

    @Override
    public String description() { return "Create fish_encyclopaedia table"; }

    @Override
    public void up(Handle handle) {
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
}