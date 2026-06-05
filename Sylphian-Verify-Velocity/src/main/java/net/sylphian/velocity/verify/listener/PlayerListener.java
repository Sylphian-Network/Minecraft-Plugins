package net.sylphian.velocity.verify.listener;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.sylphian.velocity.verify.VerifyVelocity;
import net.sylphian.velocity.verify.model.PlayerIdentity;
import net.sylphian.velocity.verify.model.VerificationResult;
import net.sylphian.velocity.verify.util.MessageUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for player-related events on the Velocity proxy to handle verification and data synchronization.
 */
public class PlayerListener {

    /** The plugin instance. */
    private final VerifyVelocity plugin;
    /** Reference to the shared map of verified player identities. */
    private final Map<UUID, PlayerIdentity> verifiedPlayers;

    /**
     * Constructs a new PlayerListener.
     *
     * @param plugin          the plugin instance
     * @param verifiedPlayers the shared identity cache
     */
    public PlayerListener(VerifyVelocity plugin, Map<UUID, PlayerIdentity> verifiedPlayers) {
        this.plugin = plugin;
        this.verifiedPlayers = verifiedPlayers;
    }

    /**
     * Intercepts the login process to verify the player's status.
     * Uses an {@link EventTask} to perform the API check asynchronously without blocking the proxy.
     *
     * @param event the login event
     * @return an EventTask that performs the check
     */
    @Subscribe
    public EventTask onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        return EventTask.async(() -> {
            try {
                // Perform the verification check
                VerificationResult result = plugin.getVerifyManager().checkPlayer(uuid, ip).join();
                if (!result.allowed()) {
                    // Deny connection if verification failed
                    event.setResult(LoginEvent.ComponentResult.denied(result.kickMessage()));
                } else {
                    // Cache the identity for plugin messaging and periodic checks
                    if (result.identity() != null) {
                        verifiedPlayers.put(uuid, result.identity());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().error("Error checking verification for {}", player.getUsername(), e);
                // Fail closed by default on technical errors
                event.setResult(LoginEvent.ComponentResult.denied(MessageUtils.buildErrorMessage(plugin.getConfig())));
            }
        });
    }

    /**
     * Synchronizes identity data to the backend server when a player successfully connects to one.
     *
     * @param event the post-connect event
     */
    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        PlayerIdentity identity = verifiedPlayers.get(player.getUniqueId());
        if (identity != null) {
            // Send the identity data as a plugin message so the backend server can sync its database
            player.getCurrentServer().ifPresent(server -> {
                String json = plugin.getGson().toJson(identity);
                server.sendPluginMessage(VerifyVelocity.IDENTIFIER, json.getBytes(StandardCharsets.UTF_8));
            });
        }
    }

    /**
     * Cleans up cached data and state when a player disconnects from the proxy.
     *
     * @param event the disconnect event
     */
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        verifiedPlayers.remove(uuid);
        plugin.getVerifyManager().resetStrikes(uuid);
    }
}
