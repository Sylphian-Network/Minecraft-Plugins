package net.sylphian.minecraft.economy.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/** Migration V1: creates the {@code sylphian_economy_balances} table. */
public class Migration001CreateBalances implements Migration {
    @Override
    public int version() { return 1; }

    @Override
    public String name() { return "CreateEconomyBalances"; }

    @Override
    public String description() {
        return "Create sylphian_economy_balances table";
    }

    @Override
    public void up(Handle handle) {
        // UUID primary key; exact-precision decimal balance defaulting to zero.
        handle.execute("""
            CREATE TABLE sylphian_economy_balances (
                player_uuid VARCHAR(36) PRIMARY KEY,
                balance DECIMAL(20, 2) NOT NULL DEFAULT 0.00
            )
        """);
    }

    @Override
    public void down(Handle handle) {
        // Drop the table to revert the migration.
        handle.execute("DROP TABLE IF EXISTS sylphian_economy_balances");
    }
}
