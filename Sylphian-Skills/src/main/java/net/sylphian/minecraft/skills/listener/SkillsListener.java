package net.sylphian.minecraft.skills.listener;

import net.sylphian.minecraft.skills.service.SkillsService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Seeds and clears the skill XP cache as players join and leave.
 */
public class SkillsListener implements Listener {

    private final SkillsService service;

    /**
     * @param service the skills service managing the XP cache
     */
    public SkillsListener(SkillsService service) {
        this.service = service;
    }

    /** Loads the player's skill data from the database into cache on join. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        service.load(event.getPlayer().getUniqueId());
    }

    /** Clears the player's cached skill data on quit. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        service.unload(event.getPlayer().getUniqueId());
    }
}
