package net.sylphian.minecraft.database.migrations;

import org.jdbi.v3.core.Jdbi;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public class MigrationRunner {
    private final Jdbi jdbi;
    private final List<Migration> migrations;
    private final String pluginName;

    public MigrationRunner(Jdbi jdbi, List<Migration> migrations, String pluginName) {
        this.jdbi = jdbi;
        this.migrations = new ArrayList<>(migrations);
        this.migrations.sort(Comparator.comparingInt(Migration::version));
        this.pluginName = pluginName;
    }

    public void run(Logger logger) {
        jdbi.useHandle(handle -> {
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

            Set<Integer> applied = new HashSet<>(
                handle.createQuery("SELECT version FROM schema_migrations WHERE plugin = :plugin")
                      .bind("plugin", pluginName)
                      .mapTo(Integer.class)
                      .list()
            );

            int newlyApplied = 0;
            for (Migration migration : migrations) {
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
        });
    }

    public void rollbackTo(int targetVersion, Logger logger) {
        jdbi.useHandle(handle -> {
            Set<Integer> applied = new HashSet<>(
                handle.createQuery("SELECT version FROM schema_migrations WHERE plugin = :plugin")
                      .bind("plugin", pluginName)
                      .mapTo(Integer.class)
                      .list()
            );

            List<Migration> toRollback = migrations.stream()
                .filter(m -> m.version() > targetVersion && applied.contains(m.version()))
                .sorted(Comparator.comparingInt(Migration::version).reversed())
                .toList();

            for (Migration migration : toRollback) {
                logger.info("[" + pluginName + "] Rolling back V" + migration.version() + " (" + migration.name() + ")");
                try {
                    migration.down(handle);
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
