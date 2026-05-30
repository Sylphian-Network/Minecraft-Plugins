package net.sylphian.minecraft.profile.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.sylphian.minecraft.profile.utils.ProfileManager;
import net.sylphian.minecraft.profile.SylphianProfile;
import net.sylphian.minecraft.profile.UserProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Listener for player chat events.
 * Responsible for applying custom chat formatting (including forum prefixes)
 * using Paper's AsyncChatEvent and ChatRenderer API.
 */
public class ChatListener implements Listener {
    private final SylphianProfile plugin;
    private final ProfileManager profileManager;

    /**
     * Constructs a new ChatListener.
     *
     * @param plugin         the plugin instance
     * @param profileManager the profile manager for retrieving cached data
     */
    public ChatListener(SylphianProfile plugin, ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
    }

    /**
     * Handles the asynchronous chat event.
     * Sets a custom ChatRenderer for the event based on the player's profile visuals.
     *
     * @param event the chat event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        UserProfile profile = profileManager.getProfile(uuid);
        // Fallback to default renderer if profile isn't loaded yet
        if (profile == null) return;

        // Apply the custom renderer from VisualManager
        event.renderer(plugin.getVisualManager().getChatRenderer(player, profile));
    }
}
