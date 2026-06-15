package net.sylphian.minecraft.clans.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/** Migration V4: creates the {@code clan_claims} table. */
public class Migration004CreateClanClaims implements Migration {

    @Override
    public int version() { return 4; }

    @Override
    public String name() { return "CreateClanClaims"; }

    @Override
    public String description() { return "Create clan_claims table"; }

    @Override
    public void up(Handle handle) {
        // Primary key is (world, chunk_x, chunk_z): one owner per chunk.
        handle.execute("""
                CREATE TABLE clan_claims (
                    world      VARCHAR(64) NOT NULL,
                    chunk_x    INT         NOT NULL,
                    chunk_z    INT         NOT NULL,
                    clan_id    CHAR(36)    NOT NULL,
                    claimed_at BIGINT      NOT NULL,
                    PRIMARY KEY (world, chunk_x, chunk_z)
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS clan_claims");
    }
}
