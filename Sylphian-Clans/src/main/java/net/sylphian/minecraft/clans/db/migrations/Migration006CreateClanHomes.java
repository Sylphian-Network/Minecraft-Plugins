package net.sylphian.minecraft.clans.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/** Migration V6: creates the {@code clan_homes} table. */
public class Migration006CreateClanHomes implements Migration {

    @Override
    public int version() { return 6; }

    @Override
    public String name() { return "CreateClanHomes"; }

    @Override
    public String description() { return "Create clan_homes table"; }

    @Override
    public void up(Handle handle) {
        handle.execute("""
                CREATE TABLE clan_homes (
                    clan_id CHAR(36)    NOT NULL PRIMARY KEY,
                    world   VARCHAR(64) NOT NULL,
                    x       DOUBLE      NOT NULL,
                    y       DOUBLE      NOT NULL,
                    z       DOUBLE      NOT NULL,
                    yaw     FLOAT       NOT NULL,
                    pitch   FLOAT       NOT NULL,
                    FOREIGN KEY (clan_id) REFERENCES clans(clan_id) ON DELETE CASCADE
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS clan_homes");
    }
}
