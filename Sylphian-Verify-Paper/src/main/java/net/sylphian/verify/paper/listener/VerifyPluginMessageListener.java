package net.sylphian.verify.paper.listener;

import com.google.gson.Gson;
import net.sylphian.verify.paper.VerifyPaper;
import net.sylphian.verify.paper.model.PlayerIdentity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class VerifyPluginMessageListener implements PluginMessageListener {
    private final VerifyPaper plugin;
    private final Gson gson;

    public VerifyPluginMessageListener(VerifyPaper plugin) {
        this.plugin = plugin;
        this.gson = plugin.getGson();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals(PlayerIdentity.CHANNEL)) {
            return;
        }

        try {
            String json = new String(message, StandardCharsets.UTF_8);
            PlayerIdentity identity = gson.fromJson(json, PlayerIdentity.class);
            if (identity != null && identity.uuid() != null) {
                plugin.writePlayerToDatabase(identity.uuid(), identity);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process plugin message from Velocity: " + e.getMessage());
        }
    }
}
