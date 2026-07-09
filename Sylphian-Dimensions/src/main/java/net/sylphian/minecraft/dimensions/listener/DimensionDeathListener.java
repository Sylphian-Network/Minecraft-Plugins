package net.sylphian.minecraft.dimensions.listener;

import net.sylphian.minecraft.dimensions.event.PlayerDeathInDimensionEvent;
import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Applies keep-inventory in managed dimensions, fires
 * {@link PlayerDeathInDimensionEvent}, and routes bed-less respawns to the hub.
 */
public class DimensionDeathListener implements Listener {

    private final DimensionManager manager;

    public DimensionDeathListener(DimensionManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Dimension dimension = manager.getDimensionByWorld(player.getWorld()).orElse(null);
        if (dimension == null) return;

        if (dimension.ruleset().keepInventory()) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }

        Bukkit.getPluginManager().callEvent(new PlayerDeathInDimensionEvent(player.getUniqueId(), dimension));
    }

    // Applies uniformly regardless of where the player died: bed/anchor wins, otherwise hub
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()) return;
        Location hub = manager.hubSpawn();
        if (hub != null) event.setRespawnLocation(hub);
    }
}
