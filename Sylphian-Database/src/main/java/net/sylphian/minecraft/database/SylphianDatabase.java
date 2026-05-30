package net.sylphian.minecraft.database;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class SylphianDatabase extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        String driverClass = config.getString("driver_class", "org.mariadb.jdbc.Driver");

        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            getLogger().severe("Database driver not found: " + driverClass);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            DatabaseService.init(
                config.getString("jdbc_url"),
                config.getString("username"),
                config.getString("password"),
                config.getInt("pool_size", 10),
                driverClass
            );

            // Register core migrations
            DatabaseService.registerMigrations(List.of(
            ));

            // Defer migration run by one tick so other plugins can register theirs first
            Bukkit.getScheduler().runTask(this, () -> {
                try {
                    DatabaseService.runMigrations("Sylphian-Database", getLogger());
                    getLogger().info("Database migrations completed successfully.");
                } catch (Exception e) {
                    getLogger().severe("Migration failed: " + e.getMessage());
                    getServer().getPluginManager().disablePlugin(this);
                }
            });

            getLogger().info("Database initialised successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialise database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        DatabaseService.shutdown();
        getLogger().info("Database shut down.");
    }
}
