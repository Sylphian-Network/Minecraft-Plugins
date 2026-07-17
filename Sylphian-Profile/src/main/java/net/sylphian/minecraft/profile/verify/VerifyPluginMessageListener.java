package net.sylphian.minecraft.profile.verify;

import com.google.gson.Gson;
import net.sylphian.minecraft.profile.service.PlayerService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for plugin messages from the Velocity proxy carrying verified player
 * identity data and syncs it to the profile database.
 */
public class VerifyPluginMessageListener implements PluginMessageListener {
    private final PlayerService playerService;
    private final Gson gson;
    private final Logger logger;

    /**
     * Constructs a new VerifyPluginMessageListener.
     *
     * @param playerService the service used to upsert the identity
     * @param gson          the Gson instance for decoding JSON payloads
     * @param logger        the plugin logger
     */
    public VerifyPluginMessageListener(PlayerService playerService, Gson gson, Logger logger) {
        this.playerService = playerService;
        this.gson = gson;
        this.logger = logger;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals(PlayerIdentity.CHANNEL)) {
            return;
        }

        try {
            String json = new String(message, StandardCharsets.UTF_8);
            PlayerIdentity identity = gson.fromJson(json, PlayerIdentity.class);
            if (identity == null) {
                return;
            }

            // Trust the connecting player's UUID over the payload: a message can be
            // forged by a client connecting directly to this backend server.
            UUID uuid = player.getUniqueId();
            if (identity.uuid() != null && !identity.uuid().equals(uuid)) {
                logger.warning("Dropping verify message for " + player.getName() + ": payload UUID " + identity.uuid() + " does not match connection UUID " + uuid);
                return;
            }

            playerService.ensurePlayerExists(uuid, identity.xfUserId(), identity.mcUsername(), identity.forumUsername())
                    .thenRun(() -> logger.info("Profile synced for player: " + uuid))
                    .exceptionally(ex -> {
                        logger.log(Level.WARNING, "Failed to sync profile for player " + uuid, ex);
                        return null;
                    });
        } catch (Exception e) {
            logger.warning("Failed to process plugin message from Velocity: " + e.getMessage());
        }
    }
}
