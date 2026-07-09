package net.sylphian.minecraft.dimensions.listener;

import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jspecify.annotations.Nullable;

/**
 * Enforces the build, damage, and PvP rules of the dimension a player is in,
 * and returns players who fall into the void to the dimension spawn.
 * Every handler early-exits on unmanaged worlds before any real work.
 */
public class DimensionProtectionListener implements Listener {

    private static final String ADMIN_PERMISSION = "sylphian.dimensions.admin";

    private final DimensionManager manager;

    public DimensionProtectionListener(DimensionManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleBuild(event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        handleBuild(event.getPlayer(), event);
    }

    private void handleBuild(Player player, Cancellable event) {
        Dimension dimension = manager.getDimensionByWorld(player.getWorld()).orElse(null);
        if (dimension == null || dimension.ruleset().buildingEnabled()) return;
        if (player.hasPermission(ADMIN_PERMISSION)) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Dimension dimension = manager.getDimensionByWorld(victim.getWorld()).orElse(null);
        if (dimension == null || dimension.ruleset().damageEnabled()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (resolvePlayerDamager(event.getDamager()) == null) return;
        Dimension dimension = manager.getDimensionByWorld(victim.getWorld()).orElse(null);
        if (dimension == null || dimension.ruleset().pvpEnabled()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to.getY() >= to.getWorld().getMinHeight()) return;

        Player player = event.getPlayer();
        Dimension dimension = manager.getDimensionByWorld(player.getWorld()).orElse(null);
        if (dimension == null) return;

        Location spawn = manager.spawnLocation(dimension);
        if (spawn == null) return;
        player.setFallDistance(0f);
        player.teleport(spawn);
    }

    private @Nullable Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) return shooter;
        return null;
    }
}
