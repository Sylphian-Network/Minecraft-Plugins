package net.sylphian.minecraft.clans.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/** Migration V2: creates the {@code sylphian_clan_members} table. */
public class Migration002CreateClanMembers implements Migration {

    @Override
    public int version() { return 2; }

    @Override
    public String name() { return "CreateClanMembers"; }

    @Override
    public String description() { return "Create sylphian_clan_members table"; }

    @Override
    public void up(Handle handle) {
        // (player_uuid, server_id) is the PK, enforcing one clan per player per server.
        // ON DELETE CASCADE removes member rows automatically when a clan is deleted.
        handle.execute("""
                CREATE TABLE sylphian_clan_members (
                    player_uuid VARCHAR(36) NOT NULL,
                    server_id   VARCHAR(64) NOT NULL,
                    clan_id     VARCHAR(36) NOT NULL,
                    is_leader   BOOLEAN     NOT NULL DEFAULT FALSE,
                    joined_at   BIGINT      NOT NULL,
                    PRIMARY KEY (player_uuid, server_id),
                    FOREIGN KEY (clan_id, server_id) REFERENCES sylphian_clans(clan_id, server_id) ON DELETE CASCADE
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS sylphian_clan_members");
    }
}
