package net.sylphian.minecraft.verify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.profile.db.models.PlayerModel;
import net.sylphian.minecraft.profile.db.repositories.PlayerRepository;
import net.sylphian.minecraft.verify.api.VerifyClient;
import net.sylphian.minecraft.verify.api.VerifyService;
import org.bukkit.configuration.file.FileConfiguration;
import net.sylphian.minecraft.verify.listener.PlayerListener;
import net.sylphian.minecraft.verify.listener.VerifyPluginMessageListener;
import net.sylphian.minecraft.verify.manager.VerifyManager;
import net.sylphian.minecraft.verify.model.PlayerIdentity;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Main plugin class for Sylphian-Verify-Paper.
 * Responsible for handling player verification on the Paper (backend) server.
 * Can operate in two modes:
 * STANDALONE: Communicates directly with the verification API.
 * PROXY: Receives verification data from the Velocity proxy via plugin messaging.
 */
public class VerifyPaper extends JavaPlugin {
    /** Singleton instance of the plugin. */
    private static VerifyPaper instance;
    /** Manager handling the core verification logic. */
    private VerifyManager verifyManager;
    /** Gson instance for JSON serialization/deserialization. */
    private Gson gson;

    /**
     * Called when the plugin is enabled.
     * Initializes the plugin based on the 'standalone' configuration setting.
     */
    @Override
    public void onEnable() {
        instance = this;
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
            getLogger().info("Sylphian-Verify-Paper enabled in STANDALONE mode");
        } else {
            // In proxy mode, we wait for data from Velocity
            getServer().getMessenger().registerIncomingPluginChannel(this, PlayerIdentity.CHANNEL, new VerifyPluginMessageListener(this));
            getLogger().info("Sylphian-Verify-Paper enabled in PROXY mode");
        }
    }

    /**
     * Gets the singleton instance of the plugin.
     * @return the VerifyPaper instance
     */
    public static VerifyPaper getInstance() {
        return instance;
    }

    /**
     * Gets the plugin configuration.
     * @return the FileConfiguration instance
     */
    public FileConfiguration getVerifyConfig() {
        return getConfig();
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
     * Writes or updates player identity data in the database.
     * This is used to ensure the profile table stays in sync with verified forum data.
     * Only executes if database support is enabled in the config and required plugins are present.
     *
     * @param uuid     the player's Mojang UUID
     * @param identity the verified identity data to write
     */
    public void writePlayerToDatabase(UUID uuid, PlayerIdentity identity) {
        // Check if database integration is active
        if (!getConfig().getBoolean("database.enabled") ||
                getServer().getPluginManager().getPlugin("Sylphian-Database") == null ||
                getServer().getPluginManager().getPlugin("Sylphian-Profile") == null) {
            getLogger().info("Database or Profile plugin is not enabled/installed. Skipping database write.");
            return;
        }

        try {
            // Use JDBI to perform async database operations
            PlayerRepository repository = new PlayerRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor());
            repository.findByUuid(uuid).thenAccept(playerOpt -> {
                long now = Instant.now().getEpochSecond();
                if (playerOpt.isPresent()) {
                    // Update existing profile with new forum identity info
                    PlayerModel existing = playerOpt.get();
                    PlayerModel updated = new PlayerModel(
                            uuid,
                            identity.xfUserId(),
                            identity.mcUsername(),
                            identity.forumUsername(),
                            existing.firstJoined(),
                            now,
                            existing.playtime(),
                            existing.isOnline()
                    );
                    repository.update(updated);
                    getLogger().info("Player data updated: " + uuid);
                } else {
                    // Create new profile for a first-time verified player
                    PlayerModel inserted = new PlayerModel(
                            uuid,
                            identity.xfUserId(),
                            identity.mcUsername(),
                            identity.forumUsername(),
                            now,
                            now,
                            0,
                            false
                    );
                    repository.insert(inserted);
                    getLogger().info("Player data inserted: " + uuid);
                }
            }).exceptionally(ex -> {
                getLogger().log(Level.WARNING, "Failed to update database for player " + uuid, ex);
                return null;
            });
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Sylphian-Database is not properly initialized or missing.", t);
        }
    }
}
