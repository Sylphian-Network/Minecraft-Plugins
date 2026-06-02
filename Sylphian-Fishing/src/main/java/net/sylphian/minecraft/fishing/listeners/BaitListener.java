package net.sylphian.minecraft.fishing.listeners;

import net.sylphian.minecraft.fishing.config.BaitConfig;
import net.sylphian.minecraft.fishing.services.BaitZoneService;
import net.sylphian.minecraft.fishing.services.bait.BaitItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handles bait item throwing and zone creation on water landing.
 *
 * <p>Right-clicking with a bait item launches a tagged snowball projectile.
 * A single shared task checks all active bait projectiles each tick for water
 * entry. When detected, the projectile is removed and a bait zone is created.
 * If the projectile hits a solid block instead, it is dropped as an item.</p>
 */
public class BaitListener implements Listener {

    private final BaitZoneService baitZoneService;
    private final JavaPlugin plugin;
    private final Map<UUID, Snowball> activeProjectiles = new HashMap<>();
    private BukkitTask trackingTask;

    /**
     * Constructs a new BaitListener.
     *
     * @param baitZoneService the service that manages active bait zones
     * @param plugin          the plugin instance for scheduling and item tagging
     */
    public BaitListener(BaitZoneService baitZoneService, JavaPlugin plugin) {
        this.baitZoneService = baitZoneService;
        this.plugin = plugin;
    }

    /**
     * Starts the shared projectile tracking task.
     * Must be called once during plugin enable.
     *
     * @param plugin the plugin instance for scheduling
     */
    public void start(JavaPlugin plugin) {
        trackingTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::trackProjectiles, 1L, 1L);
    }

    /**
     * Cancels the tracking task and clears all active projectiles.
     * Must be called during plugin disable.
     */
    public void shutdown() {
        if (trackingTask != null) trackingTask.cancel();
        activeProjectiles.clear();
    }

    /**
     * Handles right-clicking with a bait item.
     * Launches a tagged snowball and registers it for water tracking.
     *
     * @param event the interact event
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        String baitId = BaitItem.getBaitId(item, plugin);
        if (baitId == null || baitZoneService.getBaitConfig(baitId) == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Snowball projectile = player.launchProjectile(Snowball.class);
        BaitItem.tagProjectile(projectile, baitId, plugin);
        item.setAmount(item.getAmount() - 1);

        activeProjectiles.put(projectile.getUniqueId(), projectile);
    }

    /**
     * Handles the bait projectile hitting a solid block.
     * Removes the projectile from tracking and drops the bait as an item.
     *
     * @param event the projectile hit event
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;

        String baitId = BaitItem.getBaitId(snowball, plugin);
        if (baitId == null) return;

        activeProjectiles.remove(snowball.getUniqueId());

        Block hitBlock = event.getHitBlock();
        if (hitBlock == null || hitBlock.getType() == Material.WATER) return;

        BaitConfig config = baitZoneService.getBaitConfig(baitId);
        if (config != null) {
            snowball.getWorld().dropItemNaturally(
                    snowball.getLocation(), BaitItem.create(config, plugin));
        }
    }

    /**
     * Checks all active bait projectiles for water entry each tick.
     * Creates a bait zone and removes the projectile when water is detected.
     * Cleans up any projectiles that are no longer valid.
     */
    private void trackProjectiles() {
        Iterator<Map.Entry<UUID, Snowball>> it = activeProjectiles.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Snowball> entry = it.next();
            Snowball projectile = entry.getValue();

            if (!projectile.isValid()) {
                it.remove();
                continue;
            }

            if (projectile.isInWater()) {
                it.remove();

                String baitId = BaitItem.getBaitId(projectile, plugin);
                BaitConfig config = baitId != null ? baitZoneService.getBaitConfig(baitId) : null;
                if (config == null) continue;

                Block block = projectile.getLocation().getBlock();
                while (block.getRelative(0, 1, 0).getType() == Material.WATER) {
                    block = block.getRelative(0, 1, 0);
                }
                Location centre = block.getLocation().add(0.5, 1.0, 0.5);

                projectile.remove();
                baitZoneService.createZone(centre, config);
            }
        }
    }
}