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
                    server_id  VARCHAR(64) NOT NULL,
                    name       VARCHAR(32) NOT NULL,
                    motd       VARCHAR(1024) NULL,
                    created_at BIGINT      NOT NULL,
                    UNIQUE KEY idx_clans_server_name (server_id, name),
                    UNIQUE KEY idx_clans_id_server (clan_id, server_id)
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS clans");
    }
}
