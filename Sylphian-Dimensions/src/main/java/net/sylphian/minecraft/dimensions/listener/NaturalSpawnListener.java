package net.sylphian.minecraft.dimensions.listener;

import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cancels vanilla environmental creature spawns in dimensions whose ruleset
 * disables natural spawning, so a dimension can drive its own spawning instead.
 * Player- and plugin-initiated spawns (breeding, eggs, golems, placed spawners,
 * {@code CUSTOM}, {@code COMMAND}) are never blocked.
 */
public class NaturalSpawnListener implements Listener {

    // Game-driven environmental spawn reasons; player/plugin reasons are absent by design
    private static final Set<SpawnReason> BLOCKED = EnumSet.of(
            SpawnReason.NATURAL,
            SpawnReason.PATROL,
            SpawnReason.RAID,
            SpawnReason.VILLAGE_INVASION,
            SpawnReason.REINFORCEMENTS,
            SpawnReason.JOCKEY,
            SpawnReason.MOUNT,
            SpawnReason.TRAP,
            SpawnReason.NETHER_PORTAL,
            SpawnReason.SLIME_SPLIT);

    private final DimensionManager manager;

    public NaturalSpawnListener(DimensionManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!BLOCKED.contains(event.getSpawnReason())) return;

        World world = event.getLocation().getWorld();
        Dimension dimension = manager.getDimensionByWorld(world).orElse(null);
        if (dimension == null || dimension.ruleset().naturalSpawning()) return;

        event.setCancelled(true);
    }
}
