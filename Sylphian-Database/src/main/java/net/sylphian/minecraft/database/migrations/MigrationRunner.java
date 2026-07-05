package net.sylphian.minecraft.database.migrations;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Handles the execution and tracking of database migrations.
 * Ensures that migrations are applied in the correct order and that
 * each migration is only applied once per plugin.
 */
public class MigrationRunner {

    private static final String LOCK_NAME = "sylphian_schema_migrations";

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
     * Executes all pending migrations while holding the distributed schema lock.
     *
     * @param logger the logger to use for reporting progress
     */
    public void run(Logger logger) {
        jdbi.useHandle(handle -> {
            acquireLock(handle);
            try {
                applyPending(handle, logger);
            } finally {
                releaseLock(handle);
            }
        });
    }

    /**
     * Applies all migrations not yet recorded for this plugin, in version order.
     * Creates the tracking table if needed. A failed migration has its down()
     * invoked, is not recorded, and the failure is rethrown.
     *
     * @param handle the open JDBI handle to run against
     * @param logger the logger to use for reporting progress
     */
    void applyPending(Handle handle, Logger logger) {
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
                logger.info("[" + pluginName + "] Applying V" + migration.version() + " (" + migration.name() + "): " + migration.description());

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
    }

    // Serializes migrations across all server instances that share this database.
    // GET_LOCK is held on this connection only; other instances block here until
    // it is released, then find the schema already up to date. Returns 1 on
    // success, 0 on timeout, NULL on error.
    private void acquireLock(Handle handle) {
        Integer locked = handle.createQuery("SELECT GET_LOCK(:name, :timeout)")
                .bind("name", LOCK_NAME)
                .bind("timeout", 120)
                .mapTo(Integer.class)
                .one();
        if (locked == null || locked != 1) {
            throw new RuntimeException("[" + pluginName + "] Could not acquire the migration lock (timed out). Aborting migrations.");
        }
    }

    private void releaseLock(Handle handle) {
        handle.createQuery("SELECT RELEASE_LOCK(:name)")
                .bind("name", LOCK_NAME)
                .mapTo(Integer.class)
                .one();
    }
}
