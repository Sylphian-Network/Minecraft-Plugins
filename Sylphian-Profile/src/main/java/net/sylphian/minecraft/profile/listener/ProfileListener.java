package net.sylphian.minecraft.profile.listener;

import net.sylphian.minecraft.profile.SylphianProfile;
import net.sylphian.minecraft.profile.service.PlayerService;
import net.sylphian.minecraft.scoreboard.services.NametagService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player join and quit events.
 * Triggers the loading and saving of player profiles and manages
 * the transition of visual elements when players enter or leave the server.
 */
public class ProfileListener implements Listener {
    private final SylphianProfile plugin;
    private final PlayerService playerService;

    /**
     * Constructs a new ProfileListener.
     *
     * @param plugin        the plugin instance
     * @param playerService the player service for handling joins and quits
     */
    public ProfileListener(SylphianProfile plugin, PlayerService playerService) {
        this.plugin = plugin;
        this.playerService = playerService;
    }

    /**
     * Handles player joining the server.
     * Sets the player's scoreboard, triggers asynchronous data loading via PlayerService,
     * and schedules visual updates once the profile is ready.
     *
     * @param event the join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Process join asynchronously
        playerService.handleJoin(player.getUniqueId(), player.getName())
            .thenAccept(profile -> Bukkit.getScheduler().runTaskLater(plugin, () ->
                // Delay visual updates slightly to ensure other plugins have finished their join logic
                plugin.getVisualManager().updateVisuals(player, profile), 2L))
            .exceptionally(ex -> {
                plugin.getLogger().severe("Error on join for " + player.getName() + ": " + ex.getMessage());
                return null;
            });
    }

    /**
     * Handles player leaving the server.
     * Cleans up visual data and triggers the asynchronous saving of session data.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        NametagService.clearNametagEntry(event.getPlayer().getName());
        playerService.handleQuit(event.getPlayer().getUniqueId())
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to save quit data for " + event.getPlayer().getName() + ": " + ex.getMessage());
                    return null;
                });
    }
}
