package net.sylphian.minecraft.clans.db.migrations;

import net.sylphian.minecraft.database.migrations.Migration;
import org.jdbi.v3.core.Handle;

/**
 * Migration V5: adds an {@code ON DELETE CASCADE} foreign key from {@code clan_claims}
 * to {@code clans}.
 *
 * <p>Before this migration, deleting a clan row left orphaned claim rows in the database
 * if the server crashed between the unclaim-all and delete-clan steps in
 * {@link net.sylphian.minecraft.clans.service.ClanService#disbandClan}. With this FK
 * in place, the database automatically removes all claim rows whenever a clan is deleted,
 * regardless of application-layer ordering.</p>
 */
public class Migration005AddClaimsFK implements Migration {

    @Override
    public int version() { return 5; }

    @Override
    public String name() { return "AddClaimsFK"; }

    @Override
    public String description() { return "Add ON DELETE CASCADE FK from clan_claims to clans"; }

    @Override
    public void up(Handle handle) {
        handle.execute("""
                ALTER TABLE clan_claims
                    ADD CONSTRAINT fk_claims_clan
                    FOREIGN KEY (clan_id) REFERENCES clans(clan_id) ON DELETE CASCADE
                """);
    }

    @Override
    public void down(Handle handle) {
        handle.execute("ALTER TABLE clan_claims DROP FOREIGN KEY fk_claims_clan");
    }
}
