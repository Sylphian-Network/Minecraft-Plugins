package net.sylphian.minecraft.scoreboard;

import net.sylphian.minecraft.scoreboard.config.ScoreboardConfig;
import net.sylphian.minecraft.scoreboard.config.ScoreboardConfigLoader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Sylphian-Scoreboard.
 * Initializes {@link ScoreboardService} and handles player join and quit
 * events directly, as the listener logic is too small to warrant a separate class.
 */
public final class SylphianScoreboard extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ScoreboardConfig scoreboardConfig = new ScoreboardConfigLoader(getConfig()).load();
        ScoreboardService.init(this, scoreboardConfig);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Sylphian Scoreboard enabled!");
    }

    @Override
    public void onDisable() {
        ScoreboardService.shutdown();
        getLogger().info("Sylphian Scoreboard disabled!");
    }

    /**
     * Assigns a managed scoreboard to the player when they join.
     *
     * @param event the join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ScoreboardService.onJoin(event.getPlayer());
    }

    /**
     * Removes the player's managed scoreboard on disconnect.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ScoreboardService.onQuit(event.getPlayer());
    }
}