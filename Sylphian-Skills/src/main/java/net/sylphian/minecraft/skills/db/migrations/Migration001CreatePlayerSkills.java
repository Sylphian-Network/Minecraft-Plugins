package net.sylphian.minecraft.skills.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/**
 * Migration V1: Creates the player_skills table.
 *
 * <p>Stores one row per (player, skill) pair. Only total accumulated XP is
 * persisted; level is always derived at runtime so it stays in sync with
 * config changes to the XP curve.</p>
 */
public class Migration001CreatePlayerSkills implements Migration {

    @Override
    public int version() { return 1; }

    @Override
    public String name() { return "CreatePlayerSkills"; }

    @Override
    public String description() { return "Create player_skills table"; }

    @Override
    public void up(Handle handle) {
        handle.execute("""
            CREATE TABLE player_skills (
                player_uuid VARCHAR(36)  NOT NULL,
                skill_id    VARCHAR(32)  NOT NULL,
                xp          BIGINT       NOT NULL DEFAULT 0,
                PRIMARY KEY (player_uuid, skill_id)
            )
        """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS player_skills");
    }
}
