package net.sylphian.minecraft.clans.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/** Migration V1: creates the {@code clans} table. */
public class Migration001CreateClans implements Migration {

    @Override
    public int version() { return 1; }

    @Override
    public String name() { return "CreateClans"; }

    @Override
    public String description() { return "Create clans table"; }

    @Override
    public void up(Handle handle) {
        handle.execute("""
                CREATE TABLE clans (
                    clan_id    CHAR(36)    NOT NULL PRIMARY KEY,
                    name       VARCHAR(32) NOT NULL UNIQUE,
                    tag        VARCHAR(6)  NOT NULL UNIQUE,
                    created_at BIGINT      NOT NULL
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS clans");
    }
}
