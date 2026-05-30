package net.sylphian.verify.velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.sylphian.verify.velocity.api.VerifyClient;
import net.sylphian.verify.velocity.api.VerifyService;
import net.sylphian.verify.velocity.listener.PlayerListener;
import net.sylphian.verify.velocity.manager.VerifyManager;
import net.sylphian.verify.velocity.model.PlayerIdentity;
import net.sylphian.verify.velocity.model.VerificationResult;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Main plugin class for Sylphian-Verify-Velocity.
 * Handles player verification at the proxy level.
 * Features a periodic verification task to ensure currently online players maintain their link status.
 */
@Plugin(
        id = "sylphian-verify-velocity",
        name = "Sylphian-Verify-Velocity",
        version = "1.0.0",
        url = "https://sylphian.net",
        authors = {"QuackieMackie"}
)
public class VerifyVelocity {
    /** The plugin messaging channel identifier. */
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from(PlayerIdentity.CHANNEL);

    /** The Velocity proxy server instance. */
    private final ProxyServer proxy;
    /** The logger instance. */
    private final Logger logger;
    /** The plugin data directory path. */
    private final Path dataDirectory;
    /** Gson instance for JSON handling. */
    private final Gson gson;
    /** Cache of verified player identities, keyed by UUID. */
    private final Map<UUID, PlayerIdentity> verifiedPlayers = new ConcurrentHashMap<>();
    /** The plugin configuration map. */
    private Map<String, Object> config;
    /** The verification manager handling logic and rate limits. */
    private VerifyManager verifyManager;

    /**
     * Constructs a new VerifyVelocity instance.
     * Uses Google Guice for dependency injection.
     *
     * @param proxy         the proxy server
     * @param logger        the logger
     * @param dataDirectory the data directory
     */
    @Inject
    public VerifyVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Handles the proxy initialization event.
     * Sets up the config, channel registrar, verification manager, and listeners.
     *
     * @param event the initialization event
     */
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Ensure the config file exists
        Path configPath = dataDirectory.resolve("config.yml");
        if (!Files.exists(configPath)) {
            try {
                Files.createDirectories(dataDirectory);
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    }
                }
            } catch (Exception e) {
                logger.error("Could not create default config", e);
            }
        }

        // Load config using SnakeYAML
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            this.config = yaml.load(in);
        } catch (Exception e) {
            logger.error("Could not load config, using empty map", e);
            this.config = Map.of();
        }

        // Register the plugin messaging channel for backend synchronization
        proxy.getChannelRegistrar().register(IDENTIFIER);

        // Initialize API client and service
        String apiKey = (String) config.getOrDefault("api_key", "");
        VerifyClient client = new VerifyClient(
                (String) config.getOrDefault("api_base_url", "http://localhost"),
                apiKey,
                gson,
                logger
        );
        VerifyService service = new VerifyService(client);
        this.verifyManager = new VerifyManager(service, config);

        // Register event listener for player joins
        proxy.getEventManager().register(this, new PlayerListener(this, verifiedPlayers));

        // Start the background re-verification task
        startVerificationTask();

        logger.info("Sylphian-Verify-Velocity initialised successfully");
    }

    /**
     * Starts a periodic task that checks the verification status of all online players.
     * If a player is no longer verified, they are kicked from the proxy.
     */
    private void startVerificationTask() {
        int interval = (Integer) config.getOrDefault("verification_interval_minutes", 10);
        if (interval <= 0) {
            logger.info("Periodic verification task is disabled (interval set to 0)");
            return;
        }

        logger.info("Scheduling verification task to run every {} minutes", interval);

        proxy.getScheduler().buildTask(this, () -> {
                    Collection<Player> allPlayers = proxy.getAllPlayers();
                    if (allPlayers.isEmpty()) return;

                    List<UUID> uuids = allPlayers.stream()
                            .map(Player::getUniqueId)
                            .collect(Collectors.toList());

                    // Perform a batch check for all online players
                    verifyManager.checkPeriodicBatch(uuids)
                            .thenAccept(results -> {
                                for (Player player : allPlayers) {
                                    if (!player.isActive()) continue;

                                    UUID uuid = player.getUniqueId();
                                    VerificationResult result = results.get(uuid);
                                    if (result == null) continue;

                                    if (!result.allowed()) {
                                        // Kick players who failed re-verification
                                        logger.warn("Player {} ({}) failed periodic verification. Disconnecting.", player.getUsername(), uuid);
                                        verifiedPlayers.remove(uuid);
                                        player.disconnect(result.kickMessage());
                                    } else if (result.identity() != null) {
                                        // Update cached identity
                                        verifiedPlayers.put(uuid, result.identity());
                                    }
                                }
                            });
                })
                .delay(interval, TimeUnit.MINUTES)
                .repeat(interval, TimeUnit.MINUTES)
                .schedule();
    }

    /**
     * Gets the proxy server instance.
     * @return the proxy server
     */
    public ProxyServer getProxy() {
        return proxy;
    }

    /**
     * Gets the Gson instance.
     * @return the Gson instance
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Gets the verification manager.
     * @return the VerifyManager instance
     */
    public VerifyManager getVerifyManager() {
        return verifyManager;
    }

    /**
     * Gets the plugin configuration.
     * @return the configuration map
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    /**
     * Gets the logger instance.
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }
}
