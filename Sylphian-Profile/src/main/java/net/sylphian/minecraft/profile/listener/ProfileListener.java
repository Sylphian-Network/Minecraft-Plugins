package net.sylphian.minecraft.profile.listener;

import net.sylphian.minecraft.profile.SylphianProfile;
import net.sylphian.minecraft.profile.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ProfileListener implements Listener {
    private final SylphianProfile plugin;
    private final PlayerService playerService;

    public ProfileListener(SylphianProfile plugin, PlayerService playerService) {
        this.plugin = plugin;
        this.playerService = playerService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setScoreboard(plugin.getScoreboard());

        playerService.handleJoin(player.getUniqueId(), player.getName())
            .thenAccept(profile -> Bukkit.getScheduler().runTaskLater(plugin, () ->
                plugin.getVisualManager().updateVisuals(player, profile), 2L))
            .exceptionally(ex -> {
                plugin.getLogger().severe("Error on join for " + player.getName() + ": " + ex.getMessage());
                return null;
            });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getVisualManager().cleanUpPlayer(event.getPlayer());
        playerService.handleQuit(event.getPlayer().getUniqueId());
    }
}
