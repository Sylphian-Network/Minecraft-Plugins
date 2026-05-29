package net.sylphian.verify.paper.listener;

import net.sylphian.verify.paper.VerifyPaper;
import net.sylphian.verify.paper.model.VerificationResult;
import net.sylphian.verify.paper.util.MessageUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.logging.Level;

public class PlayerListener implements Listener {
    private final VerifyPaper plugin;

    public PlayerListener(VerifyPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (plugin.getVerifyManager() == null) {
            return;
        }
        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();

        try {
            VerificationResult result = plugin.getVerifyManager().checkPlayer(uuid, ip).join();
            if (!result.allowed()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, result.kickMessage());
            } else {
                // Immediately write to database before returning
                plugin.writePlayerToDatabase(uuid, result.identity());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking verification for " + event.getName(), e);
            if (plugin.getVerifyConfig().getBoolean("strike_on_api_failure", true)) {
                plugin.getVerifyManager().addStrike(uuid);
            }
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageUtils.buildErrorMessage(plugin.getVerifyConfig()));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (plugin.getVerifyManager() != null) {
            plugin.getVerifyManager().resetStrikes(uuid);
        }
    }
}
