package net.sylphian.minecraft.dimensions.listener;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import net.sylphian.minecraft.dimensions.event.PlayerEnterDimensionEvent;
import net.sylphian.minecraft.dimensions.event.PlayerExitDimensionEvent;
import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Fires dimension enter/exit events on world change, join, and quit, and
 * enforces the login rule: a player's last-known location never resolves
 * inside a dimension with {@code login-redirect} set. First-time joins start at the hub.
 */
public class PlayerConnectionListener implements Listener {

    private final DimensionManager manager;

    public PlayerConnectionListener(DimensionManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        Location hub = manager.hubSpawn();
        if (hub == null) return;

        if (event.isNewPlayer()) {
            event.setSpawnLocation(hub);
            return;
        }

        World world = event.getSpawnLocation().getWorld();
        if (world == null) return;
        Dimension dimension = manager.getDimensionByWorld(world).orElse(null);
        if (dimension != null && dimension.ruleset().loginRedirect()) {
            event.setSpawnLocation(hub);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        manager.getDimensionByWorld(player.getWorld()).ifPresent(dimension ->
                Bukkit.getPluginManager().callEvent(new PlayerEnterDimensionEvent(player.getUniqueId(), dimension)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        manager.getDimensionByWorld(player.getWorld()).ifPresent(dimension ->
                Bukkit.getPluginManager().callEvent(new PlayerExitDimensionEvent(player.getUniqueId(), dimension)));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Dimension from = manager.getDimensionByWorld(event.getFrom()).orElse(null);
        Dimension to = manager.getDimensionByWorld(player.getWorld()).orElse(null);
        if (from == to) return;

        if (from != null) {
            Bukkit.getPluginManager().callEvent(new PlayerExitDimensionEvent(player.getUniqueId(), from));
        }
        if (to != null) {
            Bukkit.getPluginManager().callEvent(new PlayerEnterDimensionEvent(player.getUniqueId(), to));
        }
    }
}
