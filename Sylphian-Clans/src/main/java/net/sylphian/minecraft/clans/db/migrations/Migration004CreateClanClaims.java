package net.sylphian.minecraft.clans.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/** Migration V4: creates the {@code sylphian_clan_claims} table. */
public class Migration004CreateClanClaims implements Migration {

    @Override
    public int version() { return 4; }

    @Override
    public String name() { return "CreateClanClaims"; }

    @Override
    public String description() { return "Create sylphian_clan_claims table"; }

    @Override
    public void up(Handle handle) {
        // Primary key is (server_id, world, chunk_x, chunk_z): one owner per chunk per server.
        handle.execute("""
                CREATE TABLE sylphian_clan_claims (
                    server_id  VARCHAR(64) NOT NULL,
                    world      VARCHAR(64) NOT NULL,
                    chunk_x    INT         NOT NULL,
                    chunk_z    INT         NOT NULL,
                    clan_id    VARCHAR(36) NOT NULL,
                    claimed_at BIGINT      NOT NULL,
                    PRIMARY KEY (server_id, world, chunk_x, chunk_z)
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS sylphian_clan_claims");
    }
}
