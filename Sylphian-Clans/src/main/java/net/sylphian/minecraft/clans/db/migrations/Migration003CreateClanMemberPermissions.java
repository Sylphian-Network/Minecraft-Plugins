package net.sylphian.minecraft.clans.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/** Migration V3: creates the {@code clan_member_permissions} table. */
public class Migration003CreateClanMemberPermissions implements Migration {

    @Override
    public int version() { return 3; }

    @Override
    public String name() { return "CreateClanMemberPermissions"; }

    @Override
    public String description() { return "Create clan_member_permissions table"; }

    @Override
    public void up(Handle handle) {
        // FK to clan_members with ON DELETE CASCADE: removing a member row (kick/leave)
        // automatically deletes that member's permission rows.
        handle.execute("""
                CREATE TABLE clan_member_permissions (
                    player_uuid CHAR(36)    NOT NULL,
                    server_id   VARCHAR(64) NOT NULL,
                    permission  VARCHAR(32) NOT NULL,
                    PRIMARY KEY (player_uuid, server_id, permission),
                    FOREIGN KEY (player_uuid, server_id) REFERENCES clan_members(player_uuid, server_id) ON DELETE CASCADE
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS clan_member_permissions");
    }
}
