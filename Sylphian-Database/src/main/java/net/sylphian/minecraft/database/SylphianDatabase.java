package net.sylphian.minecraft.database;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main plugin class for Sylphian-Database.
 * Responsible for initializing the global database connection pool and managing
 * the lifecycle of the DatabaseService. It handles driver loading, configuration,
 * and runs its own migrations immediately during onEnable.
 */
public final class SylphianDatabase extends JavaPlugin {

    /**
     * Called when the plugin is enabled.
     * Loads configuration, initializes the database connection pool via DatabaseService,
     * and runs all registered migrations immediately.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        String driverClass = config.getString("driver_class", "org.mariadb.jdbc.Driver");

        // Attempt to load the database driver class to ensure it's available in the classpath
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            getLogger().severe("Database driver not found: " + driverClass);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            // Initialize the shared database connection pool and JDBI instance
            DatabaseService.init(
                config.getString("jdbc_url"),
                config.getString("username"),
                config.getString("password"),
                config.getInt("pool_size", 10),
                driverClass
            );

            // Register migrations for the Sylphian-Database plugin itself
            DatabaseService.registerMigrations(List.of(
            ));

            DatabaseService.runMigrations("Sylphian-Database", getLogger());
            getLogger().info("Database migrations completed successfully.");

            getLogger().info("Database initialised successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialise database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Called when the plugin is disabled.
     * Gracefully shuts down the connection pool and executor service.
     */
    @Override
    public void onDisable() {
        DatabaseService.shutdown();
        getLogger().info("Database shut down.");
    }
}
