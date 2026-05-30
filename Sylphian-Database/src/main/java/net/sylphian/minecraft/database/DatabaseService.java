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

public class DatabaseService {
    private static HikariDataSource dataSource;
    private static Jdbi jdbi;
    private static ExecutorService executor;

    private static final List<Migration> pendingMigrations = new ArrayList<>();

    public static void init(String jdbcUrl, String username, String password,
                            int poolSize, String driverClass) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClass);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());

        executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "Sylphian-DB-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public static void registerMigrations(List<Migration> migrations) {
        pendingMigrations.addAll(migrations);
    }

    public static void runMigrations(String pluginName, java.util.logging.Logger logger) {
        List<Migration> all = new ArrayList<>(pendingMigrations);
        new MigrationRunner(jdbi, all, pluginName).run(logger);
        pendingMigrations.clear();
    }

    public static void shutdown() {
        if (executor != null) executor.shutdown();
        if (dataSource != null) dataSource.close();
    }

    public static DataSource getDataSource() { return dataSource; }
    public static Jdbi getJdbi() { return jdbi; }
    public static ExecutorService getExecutor() { return executor; }
}
