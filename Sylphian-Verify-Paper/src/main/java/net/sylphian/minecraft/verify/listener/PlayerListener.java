package net.sylphian.minecraft.verify.listener;

import net.sylphian.minecraft.verify.VerifyPaper;
import net.sylphian.minecraft.verify.model.VerificationResult;
import net.sylphian.minecraft.verify.util.MessageUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Listens for player connection and disconnection events on the Paper server.
 * This listener is only registered when the plugin is in STANDALONE mode.
 */
public class PlayerListener implements Listener {
    /** The plugin instance. */
    private final VerifyPaper plugin;

    /**
     * Constructs a new PlayerListener.
     * @param plugin the plugin instance
     */
    public PlayerListener(VerifyPaper plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the asynchronous pre-login event to verify players before they join.
     * Blocks the thread until the API response is received or timed out.
     *
     * @param event the pre-login event
     */
    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        // Skip if manager is not initialized (e.g., in PROXY mode)
        if (plugin.getVerifyManager() == null) {
            return;
        }
        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();

        try {
            // Check verification status via the manager (blocks because this event is async)
            VerificationResult result = plugin.getVerifyManager().checkPlayer(uuid, ip).join();
            if (!result.allowed()) {
                // Deny login if verification failed
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, result.kickMessage());
            } else {
                // Sync verified identity data to the database
                plugin.writePlayerToDatabase(uuid, result.identity());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking verification for " + event.getName(), e);
            
            // Optionally track failures as 'strikes' to prevent API spam during outages
            if (plugin.getVerifyConfig().getBoolean("strike_on_api_failure", true)) {
                plugin.getVerifyManager().addStrike(uuid);
            }
            
            // Fail closed by default if the API check crashes
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageUtils.buildErrorMessage(plugin.getVerifyConfig()));
        }
    }

    /**
     * Cleans up verification state when a player leaves the server.
     *
     * @param event the player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (plugin.getVerifyManager() != null) {
            // Reset failure strikes to allow the player to try joining again later
            plugin.getVerifyManager().resetStrikes(uuid);
        }
    }
}
