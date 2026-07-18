package net.sylphian.minecraft.clans.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/** Migration V3: creates the {@code sylphian_clan_member_permissions} table. */
public class Migration003CreateClanMemberPermissions implements Migration {

    @Override
    public int version() { return 3; }

    @Override
    public String name() { return "CreateClanMemberPermissions"; }

    @Override
    public String description() { return "Create sylphian_clan_member_permissions table"; }

    @Override
    public void up(Handle handle) {
        // FK to sylphian_clan_members with ON DELETE CASCADE: removing a member row (kick/leave)
        // automatically deletes that member's permission rows.
        handle.execute("""
                CREATE TABLE sylphian_clan_member_permissions (
                    player_uuid VARCHAR(36) NOT NULL,
                    server_id   VARCHAR(64) NOT NULL,
                    permission  VARCHAR(32) NOT NULL,
                    PRIMARY KEY (player_uuid, server_id, permission),
                    FOREIGN KEY (player_uuid, server_id) REFERENCES sylphian_clan_members(player_uuid, server_id) ON DELETE CASCADE
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS sylphian_clan_member_permissions");
    }
}
