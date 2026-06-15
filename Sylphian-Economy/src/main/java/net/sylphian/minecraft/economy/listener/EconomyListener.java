package net.sylphian.minecraft.economy.listener;

import net.sylphian.minecraft.economy.service.EconomyService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/** Ensures every joining player has an economy account row. */
public class EconomyListener implements Listener {

    private final EconomyService economyService;
    private final Plugin plugin;

    /**
     * @param economyService the service used to seed accounts
     */
    public EconomyListener(EconomyService economyService, Plugin plugin) {
        this.economyService = economyService;
        this.plugin = plugin;
    }

    /**
     * Seeds the player's account on join. Non-blocking; the work runs async in the service.
     *
     * @param event the join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        economyService.load(event.getPlayer().getUniqueId())
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to seed economy account for "
                            + event.getPlayer().getName() + ": " + ex.getMessage());
                    return null;
                });
    }
}
