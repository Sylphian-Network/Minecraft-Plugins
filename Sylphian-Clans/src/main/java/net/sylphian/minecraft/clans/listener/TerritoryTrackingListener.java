package net.sylphian.minecraft.clans.listener;

import net.sylphian.minecraft.clans.event.ClanTerritoryEnterEvent;
import net.sylphian.minecraft.clans.event.ClanTerritoryExitEvent;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Watches player movement and fires {@link ClanTerritoryExitEvent} and
 * {@link ClanTerritoryEnterEvent} when a player crosses a chunk boundary into a
 * differently-owned chunk. Holds no display logic; consumers react to the events.
 */
public class TerritoryTrackingListener implements Listener {

    private final TerritoryService territoryService;

    /**
     * @param territoryService used to resolve chunk ownership from the cache
     */
    public TerritoryTrackingListener(TerritoryService territoryService) {
        this.territoryService = territoryService;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;

        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        int fromX = event.getFrom().getChunk().getX();
        int fromZ = event.getFrom().getChunk().getZ();
        int toX   = event.getTo().getChunk().getX();
        int toZ   = event.getTo().getChunk().getZ();

        Optional<UUID> fromClan = territoryService.getClaimingClan(world, fromX, fromZ);
        Optional<UUID> toClan   = territoryService.getClaimingClan(world, toX, toZ);

        if (fromClan.equals(toClan)) return;

        UUID playerId = player.getUniqueId();
        fromClan.ifPresent(c -> Bukkit.getPluginManager().callEvent(
                new ClanTerritoryExitEvent(playerId, c, world, fromX, fromZ)));
        toClan.ifPresent(c -> Bukkit.getPluginManager().callEvent(
                new ClanTerritoryEnterEvent(playerId, c, world, toX, toZ)));
    }
}
