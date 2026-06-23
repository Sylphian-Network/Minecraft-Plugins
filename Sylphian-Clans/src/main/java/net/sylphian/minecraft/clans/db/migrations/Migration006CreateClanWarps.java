package net.sylphian.minecraft.clans.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/** Migration V6: replaces the legacy clan_homes table with clan_warps and clan_warp_access. */
public class Migration006CreateClanWarps implements Migration {

    @Override
    public int version() { return 6; }

    @Override
    public String name() { return "CreateClanWarps"; }

    @Override
    public String description() { return "Create clan_warps and clan_warp_access tables"; }

    @Override
    public void up(Handle handle) {
        // Remove the superseded single-home table if it exists (clean upgrade from the home system).
        handle.execute("DROP TABLE IF EXISTS clan_homes");

        handle.execute("""
                CREATE TABLE clan_warps (
                    clan_id     CHAR(36)     NOT NULL,
                    name        VARCHAR(32)  NOT NULL,
                    world       VARCHAR(64)  NOT NULL,
                    x           DOUBLE       NOT NULL,
                    y           DOUBLE       NOT NULL,
                    z           DOUBLE       NOT NULL,
                    yaw         FLOAT        NOT NULL,
                    pitch       FLOAT        NOT NULL,
                    icon        VARCHAR(64)  NOT NULL,
                    description VARCHAR(256) NOT NULL,
                    restricted  BOOLEAN      NOT NULL DEFAULT 0,
                    PRIMARY KEY (clan_id, name),
                    FOREIGN KEY (clan_id) REFERENCES clans(clan_id) ON DELETE CASCADE
                )
                """);

        handle.execute("""
                CREATE TABLE clan_warp_access (
                    clan_id     CHAR(36)    NOT NULL,
                    warp_name   VARCHAR(32) NOT NULL,
                    player_uuid CHAR(36)    NOT NULL,
                    PRIMARY KEY (clan_id, warp_name, player_uuid),
                    FOREIGN KEY (clan_id, warp_name) REFERENCES clan_warps(clan_id, name) ON DELETE CASCADE
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS clan_warp_access");
        handle.execute("DROP TABLE IF EXISTS clan_warps");
    }
}
