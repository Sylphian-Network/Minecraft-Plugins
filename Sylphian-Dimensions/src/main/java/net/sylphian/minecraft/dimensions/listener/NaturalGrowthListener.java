package net.sylphian.minecraft.dimensions.listener;

import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.world.StructureGrowEvent;

/**
 * Freezes vanilla plant growth and spread in dimensions whose ruleset disables
 * natural growth, covering crop and sapling maturing, bonemeal, tree growth, and
 * grass, vine, mushroom, and fire spread.
 */
public class NaturalGrowthListener implements Listener {

    private final DimensionManager manager;

    public NaturalGrowthListener(DimensionManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        cancelIfFrozen(event.getBlock().getWorld(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        cancelIfFrozen(event.getBlock().getWorld(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        cancelIfFrozen(event.getBlock().getWorld(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        cancelIfFrozen(event.getLocation().getWorld(), event);
    }

    private void cancelIfFrozen(World world, Cancellable event) {
        Dimension dimension = manager.getDimensionByWorld(world).orElse(null);
        if (dimension == null || dimension.ruleset().naturalGrowth()) return;
        event.setCancelled(true);
    }
}
