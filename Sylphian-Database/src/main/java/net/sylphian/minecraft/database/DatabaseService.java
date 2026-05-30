package net.sylphian.minecraft.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.sylphian.minecraft.database.migrations.Migration;
import net.sylphian.minecraft.database.migrations.MigrationRunner;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Core service layer providing database access and migration management.
 * Acts as a singleton provider for the HikariDataSource, JDBI instance,
 * and a shared async executor service used by downstream plugins for
 * non-blocking database operations.
 */
public class DatabaseService {
    private static HikariDataSource dataSource;
    private static Jdbi jdbi;
    private static ExecutorService executor;

    private static final List<Migration> pendingMigrations = new ArrayList<>();

    /**
     * Initializes the database connection pool and JDBI instance.
     * Also sets up a fixed thread pool for asynchronous database tasks.
     *
     * @param jdbcUrl     the JDBC connection string
     * @param username    the database user
     * @param password    the database password
     * @param poolSize    maximum number of connections in the pool
     * @param driverClass the fully qualified name of the JDBC driver class
     */
    public static void init(String jdbcUrl, String username, String password,
                            int poolSize, String driverClass) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClass);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        // Performance optimizations for prepared statements
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());

        // Executor service for async operations, using a daemon thread factory to avoid hanging JVM on shutdown
        executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "Sylphian-DB-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Registers a list of migrations to be executed.
     * Plugins should call this during their onEnable phase.
     *
     * @param migrations the list of migration instances to add to the queue
     */
    public static void registerMigrations(List<Migration> migrations) {
        pendingMigrations.addAll(migrations);
    }

    /**
     * Executes all registered migrations using the MigrationRunner.
     *
     * @param pluginName the name of the plugin triggering the migrations (used for logging and tracking)
     * @param logger     the logger instance for reporting progress/errors
     */
    public static void runMigrations(String pluginName, java.util.logging.Logger logger) {
        List<Migration> all = new ArrayList<>(pendingMigrations);
        new MigrationRunner(jdbi, all, pluginName).run(logger);
        pendingMigrations.clear();
    }

    /**
     * Shuts down the connection pool and the async executor service.
     */
    public static void shutdown() {
        if (executor != null) executor.shutdown();
        if (dataSource != null) dataSource.close();
    }

    /** @return the HikariDataSource instance */
    public static DataSource getDataSource() { return dataSource; }

    /** @return the JDBI instance configured with SqlObject support */
    public static Jdbi getJdbi() { return jdbi; }

    /** @return the shared async executor service for database tasks */
    public static ExecutorService getExecutor() { return executor; }
}
