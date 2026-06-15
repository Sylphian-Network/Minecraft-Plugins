package net.sylphian.minecraft.verify.listener;

import net.kyori.adventure.text.Component;
import net.sylphian.minecraft.verify.VerifyPaper;
import net.sylphian.minecraft.verify.model.VerificationResult;
import net.sylphian.minecraft.verify.util.MessageUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
            int timeoutSeconds = plugin.getVerifyConfig().getInt("api_timeout_seconds", 5);
            VerificationResult result = plugin.getVerifyManager().checkPlayer(uuid, ip).get(timeoutSeconds, TimeUnit.SECONDS);
            if (!result.allowed()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, result.kickMessage());
            } else {
                plugin.writePlayerToDatabase(uuid, result.identity());
            }
        } catch (TimeoutException e) {
            plugin.getLogger().warning("Verification timed out for " + event.getName() + " — denying login.");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Verification timed out — please try again."));
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.SEVERE, "Verification check failed for " + event.getName(), e.getCause());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageUtils.buildErrorMessage(plugin.getVerifyConfig()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
