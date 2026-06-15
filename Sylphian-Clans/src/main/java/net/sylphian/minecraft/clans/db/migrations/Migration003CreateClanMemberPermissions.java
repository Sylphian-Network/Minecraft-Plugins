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
        // No FK to clan_members: ClanService deletes permission rows explicitly
        // when a member is kicked or leaves, keeping cascade logic in the service layer.
        handle.execute("""
                CREATE TABLE clan_member_permissions (
                    player_uuid CHAR(36)    NOT NULL,
                    permission  VARCHAR(32) NOT NULL,
                    PRIMARY KEY (player_uuid, permission)
                )
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS clan_member_permissions");
    }
}
