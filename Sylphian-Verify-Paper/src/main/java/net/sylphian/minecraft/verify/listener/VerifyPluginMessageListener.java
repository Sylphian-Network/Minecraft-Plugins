package net.sylphian.minecraft.verify.listener;

import com.google.gson.Gson;
import net.sylphian.minecraft.verify.VerifyPaper;
import net.sylphian.minecraft.verify.model.PlayerIdentity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * Listens for plugin messages from the Velocity proxy.
 * This is used in PROXY mode to receive verified player identity data
 * and sync it to the local database without making additional API calls.
 */
public class VerifyPluginMessageListener implements PluginMessageListener {
    /** The plugin instance. */
    private final VerifyPaper plugin;
    /** Gson instance for decoding JSON messages. */
    private final Gson gson;

    /**
     * Constructs a new VerifyPluginMessageListener.
     * @param plugin the plugin instance
     */
    public VerifyPluginMessageListener(VerifyPaper plugin) {
        this.plugin = plugin;
        this.gson = plugin.getGson();
    }

    /**
     * Handles incoming plugin messages on the verification channel.
     *
     * @param channel the channel name
     * @param player  the player associated with the message
     * @param message the raw byte array message
     */
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        // Only process messages on our designated channel
        if (!channel.equals(PlayerIdentity.CHANNEL)) {
            return;
        }

        try {
            // Decode the UTF-8 JSON payload
            String json = new String(message, StandardCharsets.UTF_8);
            PlayerIdentity identity = gson.fromJson(json, PlayerIdentity.class);
            
            if (identity != null && identity.uuid() != null) {
                // Update the player's profile in the database with the proxy-provided data
                plugin.writePlayerToDatabase(identity.uuid(), identity);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process plugin message from Velocity: " + e.getMessage());
        }
    }
}
