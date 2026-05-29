package net.sylphian.verify.paper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.profile.db.models.PlayerModel;
import net.sylphian.minecraft.profile.db.repositories.PlayerRepository;
import net.sylphian.verify.paper.api.VerifyClient;
import net.sylphian.verify.paper.api.VerifyService;
import org.bukkit.configuration.file.FileConfiguration;
import net.sylphian.verify.paper.listener.PlayerListener;
import net.sylphian.verify.paper.listener.VerifyPluginMessageListener;
import net.sylphian.verify.paper.manager.VerifyManager;
import net.sylphian.verify.paper.model.PlayerIdentity;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

public class VerifyPaper extends JavaPlugin {
    private static VerifyPaper instance;
    private VerifyManager verifyManager;
    private Gson gson;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        this.gson = new GsonBuilder().create();

        boolean standalone = config.getBoolean("standalone", true);

        if (standalone) {
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
            getServer().getMessenger().registerIncomingPluginChannel(this, PlayerIdentity.CHANNEL, new VerifyPluginMessageListener(this));
            getLogger().info("Sylphian-Verify-Paper enabled in PROXY mode");
        }
    }

    public static VerifyPaper getInstance() {
        return instance;
    }

    public FileConfiguration getVerifyConfig() {
        return getConfig();
    }

    public VerifyManager getVerifyManager() {
        return verifyManager;
    }

    public Gson getGson() {
        return gson;
    }

    public void writePlayerToDatabase(UUID uuid, PlayerIdentity identity) {
        if (!getConfig().getBoolean("database.enabled") ||
                getServer().getPluginManager().getPlugin("Sylphian-Database") == null ||
                getServer().getPluginManager().getPlugin("Sylphian-Profile") == null) {
            getLogger().info("Database or Profile plugin is not enabled/installed. Skipping database write.");
            return;
        }

        try {
            PlayerRepository repository = new PlayerRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor());
            repository.findByUuid(uuid).thenAccept(playerOpt -> {
                long now = Instant.now().getEpochSecond();
                if (playerOpt.isPresent()) {
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
