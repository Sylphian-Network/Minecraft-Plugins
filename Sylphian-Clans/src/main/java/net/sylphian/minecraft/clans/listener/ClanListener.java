package net.sylphian.minecraft.clans.listener;

import net.sylphian.minecraft.clans.service.ClanService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player lifecycle events to keep {@link net.sylphian.minecraft.clans.cache.ClanCache}
 * populated, seeding on join and evicting on quit so the cache stays bounded by online players.
 */
public class ClanListener implements Listener {

    private final ClanService clanService;

    /**
     * @param clanService the clan service used to seed and evict the cache
     */
    public ClanListener(ClanService clanService) {
        this.clanService = clanService;
    }

    /**
     * Seeds the clan cache for the joining player.
     *
     * @param event the join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        clanService.seedCacheForPlayer(event.getPlayer().getUniqueId());
    }

    /**
     * Evicts the quitting player's clan snapshot from the cache.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        clanService.releaseCacheForPlayer(event.getPlayer().getUniqueId());
    }
}
