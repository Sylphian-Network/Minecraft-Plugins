package net.sylphian.minecraft.verify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sylphian.minecraft.profile.api.ProfileProvider;
import net.sylphian.minecraft.verify.api.VerifyClient;
import net.sylphian.minecraft.verify.api.VerifyService;
import org.bukkit.configuration.file.FileConfiguration;
import net.sylphian.minecraft.verify.listener.PlayerListener;
import net.sylphian.minecraft.verify.listener.VerifyPluginMessageListener;
import net.sylphian.minecraft.verify.manager.VerifyManager;
import net.sylphian.minecraft.verify.model.PlayerIdentity;
import net.sylphian.minecraft.verify.model.VerificationResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Main plugin class for Sylphian-Verify-Paper.
 * Responsible for handling player verification on the Paper (backend) server.
 * Can operate in two modes:
 * STANDALONE: Communicates directly with the verification API.
 * PROXY: Receives verification data from the Velocity proxy via plugin messaging.
 */
public class VerifyPaper extends JavaPlugin {
    /** Manager handling the verification logic. */
    private VerifyManager verifyManager;
    /** Gson instance for JSON serialization/deserialization. */
    private Gson gson;

    /**
     * Called when the plugin is enabled.
     * Initializes the plugin based on the 'standalone' configuration setting.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        this.gson = new GsonBuilder().create();

        boolean standalone = config.getBoolean("standalone", true);

        if (standalone) {
            // In standalone mode, we handle our own API calls and player join events
            String apiKey = config.getString("api_key", "");
            VerifyClient client = new VerifyClient(
                    config.getString("api_base_url", "http://localhost"),
                    apiKey,
                    gson,
                    getLogger()
            );
            VerifyService service = new VerifyService(client);
            this.verifyManager = new VerifyManager(service, config);

            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            startVerificationTask();
            getLogger().info("Sylphian-Verify-Paper enabled in STANDALONE mode");
        } else {
            // In proxy mode, we wait for data from Velocity
            getServer().getMessenger().registerIncomingPluginChannel(this, PlayerIdentity.CHANNEL, new VerifyPluginMessageListener(this));
            getLogger().info("Sylphian-Verify-Paper enabled in PROXY mode");
        }
    }

    /**
     * Gets the plugin configuration.
     * @return the FileConfiguration instance
     */
    public FileConfiguration getVerifyConfig() {
        return getConfig();
    }

    /**
     * Starts a periodic task that batch-checks the verification status of all online players.
     * If a player is no longer verified, they are kicked from the server.
     * Runs asynchronously on Bukkit's scheduler. Kicks are dispatched back to the main thread.
     * Only called in STANDALONE mode.
     */
    private void startVerificationTask() {
        int interval = getConfig().getInt("verification_interval_minutes", 10);
        if (interval <= 0) {
            getLogger().info("Periodic verification task is disabled (interval <= 0).");
            return;
        }

        long intervalTicks = interval * 60L * 20L;
        getLogger().info("Scheduling periodic verification task every " + interval + " minute(s).");

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers.isEmpty()) return;

            List<UUID> uuids = onlinePlayers.stream()
                    .map(Player::getUniqueId)
                    .collect(Collectors.toList());

            verifyManager.checkPeriodicBatch(uuids).thenAccept(results -> {
                for (Player player : onlinePlayers) {
                    if (!player.isOnline()) continue;

                    UUID uuid = player.getUniqueId();
                    VerificationResult result = results.get(uuid);
                    if (result == null || result.allowed()) continue;

                    getLogger().warning("Player " + player.getName() + " (" + uuid + ") failed periodic verification. Disconnecting.");
                    // Kicks must happen on the main thread
                    getServer().getScheduler().runTask(this, () -> player.kick(result.kickMessage()));
                }
            });
        }, intervalTicks, intervalTicks);
    }

    /**
     * Gets the verification manager.
     * @return the VerifyManager instance, or null if in PROXY mode
     */
    public VerifyManager getVerifyManager() {
        return verifyManager;
    }

    /**
     * Gets the Gson instance.
     * @return the Gson instance
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Writes or updates player identity data in the database via the ProfileProvider API.
     * Only executes if database support is enabled in the config and Sylphian-Profile is loaded.
     *
     * @param uuid     the player's Mojang UUID
     * @param identity the verified identity data to write
     */
    public void writePlayerToDatabase(UUID uuid, PlayerIdentity identity) {
        if (!getConfig().getBoolean("database.enabled")) {
            return;
        }
        if (!ProfileProvider.isAvailable()) {
            getLogger().info("Sylphian-Profile is not loaded — skipping profile write for " + uuid);
            return;
        }
        ProfileProvider.get()
                .ensurePlayerExists(uuid, identity.xfUserId(), identity.mcUsername(), identity.forumUsername())
                .thenRun(() -> getLogger().info("Profile synced for player: " + uuid))
                .exceptionally(ex -> {
                    getLogger().log(Level.WARNING, "Failed to sync profile for player " + uuid, ex);
                    return null;
                });
    }
}
