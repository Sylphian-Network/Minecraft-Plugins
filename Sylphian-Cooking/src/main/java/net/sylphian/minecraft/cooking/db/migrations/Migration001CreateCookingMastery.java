package net.sylphian.minecraft.cooking.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/**
 * Migration V1: Creates the cooking_mastery table.
 */
public class Migration001CreateCookingMastery implements Migration {

    @Override
    public int version() { return 1; }

    @Override
    public String name() { return "CreateCookingMastery"; }

    @Override
    public String description() { return "Create cooking_mastery table"; }

    @Override
    public void up(Handle handle) {
        handle.execute("""
            CREATE TABLE IF NOT EXISTS cooking_mastery (
                player_uuid VARCHAR(36)  NOT NULL,
                recipe_id   VARCHAR(64)  NOT NULL,
                cook_count  INT          NOT NULL DEFAULT 0,
                PRIMARY KEY (player_uuid, recipe_id)
            )
        """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("DROP TABLE IF EXISTS cooking_mastery");
    }
}
