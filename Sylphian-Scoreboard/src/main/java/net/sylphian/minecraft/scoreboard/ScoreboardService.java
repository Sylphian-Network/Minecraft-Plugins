package net.sylphian.minecraft.scoreboard;

import net.sylphian.minecraft.scoreboard.config.ScoreboardConfig;
import net.sylphian.minecraft.scoreboard.services.NametagService;

import net.sylphian.minecraft.scoreboard.services.SidebarService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

/**
 * Core service for Sylphian-Scoreboard.
 *
 * <p>Owns the per-player scoreboard map and manages the full lifecycle —
 * initialization, player join/quit handling, and the periodic refresh task.
 * All scoreboard interactions from other plugins go through the sub-services
 * {@link SidebarService} and {@link NametagService}, which reach per-player
 * scoreboards via {@link #getScoreboard(UUID)}.</p>
 */
public class ScoreboardService {

    private static final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private static BukkitTask refreshTask;

    private ScoreboardService() {}

    /**
     * Configures sub-services and starts the periodic refresh task.
     * Assigns scoreboards to any already-online players immediately.
     * Must be called before players can join.
     *
     * @param plugin the plugin instance used for scheduling
     * @param config the parsed scoreboard configuration
     */
    public static void init(JavaPlugin plugin, ScoreboardConfig config) {
        SidebarService.configure(config);
        for (Player player : Bukkit.getOnlinePlayers()) {
            onJoin(player);
        }
        refreshTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, ScoreboardService::refreshAll, 0L, config.updateIntervalTicks());
    }

    /**
     * Shuts down the service — cancels the refresh task, restores all players
     * to the main scoreboard, and clears all state in both sub-services.
     */
    public static void shutdown() {
        if (refreshTask != null) refreshTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(
                    Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard());
        }
        playerScoreboards.clear();
        SidebarService.clear();
        NametagService.clear();
    }

    /**
     * Creates and assigns a fresh scoreboard for a joining player.
     * Applies all existing sidebar and nametag state immediately.
     *
     * @param player the joining player
     */
    public static void onJoin(Player player) {
        Scoreboard scoreboard = Objects.requireNonNull(
                Bukkit.getScoreboardManager()).getNewScoreboard();

        SidebarService.setup(scoreboard);
        NametagService.applyToScoreboard(scoreboard);

        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
        SidebarService.update(scoreboard, player);
    }

    /**
     * Removes the player's scoreboard on quit.
     *
     * @param player the quitting player
     */
    public static void onQuit(Player player) {
        playerScoreboards.remove(player.getUniqueId());
    }

    /**
     * Immediately refreshes the sidebar for a single player.
     *
     * @param player the player to refresh
     */
    public static void refresh(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;
        SidebarService.update(scoreboard, player);
    }

    /**
     * Called by the periodic refresh task at the configured update interval.
     */
    public static void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    /**
     * Returns the managed scoreboard for the given player UUID.
     * Used by sub-services to reach player scoreboards — not intended
     * for external plugins.
     *
     * @param uuid the player UUID
     * @return the scoreboard, or null if the player is not tracked
     */
    public static Scoreboard getScoreboard(UUID uuid) {
        return playerScoreboards.get(uuid);
    }
}