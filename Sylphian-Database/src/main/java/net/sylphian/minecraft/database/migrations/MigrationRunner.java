package net.sylphian.minecraft.database.migrations;

import org.jdbi.v3.core.Jdbi;

import java.time.Instant;
import java.util.*;

public class MigrationRunner {
    private final Jdbi jdbi;
    private final List<Migration> migrations;

    public MigrationRunner(Jdbi jdbi, List<Migration> migrations) {
        this.jdbi = jdbi;
        this.migrations = new ArrayList<>(migrations);
        this.migrations.sort(Comparator.comparingInt(Migration::version));
    }

    public void run(java.util.logging.Logger logger) {
        jdbi.useHandle(handle -> {
            handle.execute("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version INT PRIMARY KEY,
                    applied_at BIGINT NOT NULL,
                    description VARCHAR(255) NOT NULL
                )
            """);

            Set<Integer> applied = new HashSet<>(
                handle.createQuery("SELECT version FROM schema_migrations")
                      .mapTo(Integer.class)
                      .list()
            );

            int newlyApplied = 0;
            for (Migration migration : migrations) {
                if (!applied.contains(migration.version())) {
                    logger.info("Applying migration V" + migration.version()
                        + " — " + migration.description());
                    migration.up(handle);
                    handle.execute(
                        "INSERT INTO schema_migrations (version, applied_at, description) VALUES (?, ?, ?)",
                        migration.version(),
                        Instant.now().getEpochSecond(),
                        migration.description()
                    );
                    logger.info("Migration V" + migration.version() + " applied successfully.");
                    newlyApplied++;
                }
            }

            if (newlyApplied == 0) {
                logger.info("Schema is up to date. No migrations to apply. ("
                    + migrations.size() + " registered)");
            } else {
                logger.info("Applied " + newlyApplied + " migration(s) successfully. Schema is now up to date.");
            }
        });
    }
}
