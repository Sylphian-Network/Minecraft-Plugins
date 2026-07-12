package net.sylphian.minecraft.dimensions.listener;

import net.sylphian.minecraft.clans.event.ClanClaimEvent;
import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Cancels clan claims inside dimensions where the {@code claiming} rule is off.
 *
 * <p>All Sylphian-Clans references are isolated here; only register this
 * listener when the Sylphian-Clans plugin is present.</p>
 */
public class ClanClaimListener implements Listener {

    private final DimensionManager manager;

    public ClanClaimListener(DimensionManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClaim(ClanClaimEvent.Pre event) {
        World world = Bukkit.getWorld(event.getWorld());
        if (world == null) return;

        Dimension dimension = manager.getDimensionByWorld(world).orElse(null);
        if (dimension == null || dimension.ruleset().claimingEnabled()) return;

        event.setCancelled(true);
    }
}
