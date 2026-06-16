package net.sylphian.minecraft.database.migrations;

import org.jdbi.v3.core.Jdbi;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * Handles the execution and tracking of database migrations.
 * Ensures that migrations are applied in the correct order and that
 * each migration is only applied once per plugin.
 */
public class MigrationRunner {
    private final Jdbi jdbi;
    private final List<Migration> migrations;
    private final String pluginName;

    /**
     * Constructs a new MigrationRunner.
     * Sorts the provided migrations by their version number to ensure sequential execution.
     *
     * @param jdbi        the JDBI instance to use
     * @param migrations  the list of migrations to consider
     * @param pluginName  the name of the plugin these migrations belong to
     */
    public MigrationRunner(Jdbi jdbi, List<Migration> migrations, String pluginName) {
        this.jdbi = jdbi;
        this.migrations = new ArrayList<>(migrations);
        this.migrations.sort(Comparator.comparingInt(Migration::version));
        this.pluginName = pluginName;
    }

    /**
     * Executes all pending migrations.
     * Creates the tracking table if it doesn't exist, checks which migrations
     * have already been applied, and executes new ones within a transaction.
     *
     * @param logger the logger to use for reporting progress
     */
    public void run(Logger logger) {
        jdbi.useHandle(handle -> {
            // Serialize migrations across all server instances that share this database.
            // GET_LOCK is held on this connection only; other instances block here until
            // it is released, then find the schema already up to date. Returns 1 on
            // success, 0 on timeout, NULL on error.
            final String lockName = "sylphian_schema_migrations";
            Integer locked = handle.createQuery("SELECT GET_LOCK(:name, :timeout)")
                    .bind("name", lockName)
                    .bind("timeout", 120)
                    .mapTo(Integer.class)
                    .one();
            if (locked == null || locked != 1) {
                throw new RuntimeException("[" + pluginName + "] Could not acquire the migration lock (timed out). Aborting migrations.");
            }

            try {
                // Ensure the migration tracking table exists before proceeding
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version INT,
                        name VARCHAR(255),
                        plugin VARCHAR(64),
                        description VARCHAR(255),
                        applied_at BIGINT,
                        execution_ms BIGINT,
                        PRIMARY KEY (plugin, version)
                    )
                """);

                // Fetch already applied versions for this specific plugin
                Set<Integer> applied = new HashSet<>(
                        handle.createQuery("SELECT version FROM schema_migrations WHERE plugin = :plugin")
                                .bind("plugin", pluginName)
                                .mapTo(Integer.class)
                                .list()
                );

                int newlyApplied = 0;
                for (Migration migration : migrations) {
                    // Only apply migrations that haven't been recorded yet
                    if (!applied.contains(migration.version())) {
                        logger.info("[" + pluginName + "] Applying V" + migration.version() + " (" + migration.name() + ") — " + migration.description());

                        long start = System.currentTimeMillis();
                        try {
                            migration.up(handle);
                        } catch (Exception e) {
                            try {
                                migration.down(handle);
                                logger.info("[" + pluginName + "] V" + migration.version() + " rolled back after failure.");
                            } catch (Exception downEx) {
                                logger.severe("[" + pluginName + "] Failed to rollback V" + migration.version() + ": " + downEx.getMessage());
                            }
                            throw new RuntimeException("Migration V" + migration.version() + " failed for plugin " + pluginName, e);
                        }
                        long executionMs = System.currentTimeMillis() - start;

                        handle.execute(
                                "INSERT INTO schema_migrations (version, name, plugin, description, applied_at, execution_ms) VALUES (?, ?, ?, ?, ?, ?)",
                                migration.version(),
                                migration.name(),
                                pluginName,
                                migration.description(),
                                Instant.now().getEpochSecond(),
                                executionMs
                        );

                        logger.info("[" + pluginName + "] V" + migration.version() + " (" + migration.name() + ") applied in " + executionMs + "ms.");
                        newlyApplied++;
                    }
                }

                if (newlyApplied == 0) {
                    logger.info("[" + pluginName + "] Schema up to date. (" + migrations.size() + " migration(s) registered)");
                }
            } finally {
                handle.createQuery("SELECT RELEASE_LOCK(:name)")
                        .bind("name", lockName)
                        .mapTo(Integer.class)
                        .one();
            }
        });
    }

    /**
     * Rolls back database schema to a specific target version.
     * Useful for debugging or reverting updates.
     *
     * @param targetVersion the version to roll back to (exclusive, migrations > targetVersion are reverted)
     * @param logger        the logger for reporting progress
     */
    public void rollbackTo(int targetVersion, Logger logger) {
        jdbi.useHandle(handle -> {
            // Identify currently applied migrations
            Set<Integer> applied = new HashSet<>(
                handle.createQuery("SELECT version FROM schema_migrations WHERE plugin = :plugin")
                      .bind("plugin", pluginName)
                      .mapTo(Integer.class)
                      .list()
            );

            // Filter migrations that need to be rolled back and sort them in reverse order (newest first)
            List<Migration> toRollback = migrations.stream()
                .filter(m -> m.version() > targetVersion && applied.contains(m.version()))
                .sorted(Comparator.comparingInt(Migration::version).reversed())
                .toList();

            for (Migration migration : toRollback) {
                logger.info("[" + pluginName + "] Rolling back V" + migration.version() + " (" + migration.name() + ")");
                try {
                    // Execute the rollback logic
                    migration.down(handle);
                    // Remove the record from the tracking table
                    handle.execute(
                        "DELETE FROM schema_migrations WHERE plugin = ? AND version = ?",
                        pluginName,
                        migration.version()
                    );
                    logger.info("[" + pluginName + "] V" + migration.version() + " rolled back successfully.");
                } catch (Exception e) {
                    logger.severe("[" + pluginName + "] Failed to rollback V" + migration.version() + ": " + e.getMessage());
                    throw new RuntimeException("Rollback of V" + migration.version() + " failed", e);
                }
            }
        });
    }
}
