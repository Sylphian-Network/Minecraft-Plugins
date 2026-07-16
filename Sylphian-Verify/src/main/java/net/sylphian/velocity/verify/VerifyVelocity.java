package net.sylphian.velocity.verify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.sylphian.velocity.verify.api.VerifyClient;
import net.sylphian.velocity.verify.api.VerifyService;
import net.sylphian.velocity.verify.listener.PlayerListener;
import net.sylphian.velocity.verify.manager.VerifyManager;
import net.sylphian.velocity.verify.model.PlayerIdentity;
import net.sylphian.velocity.verify.model.VerificationResult;
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
 * Main plugin class for Sylphian-Verify.
 * Handles player verification at the proxy level.
 * Features a periodic verification task to ensure currently online players maintain their link status.
 */
@Plugin(
        id = "sylphian-verify",
        name = "Sylphian-Verify",
        version = BuildConstants.VERSION,
        url = "https://sylphian.net",
        authors = {"QuackieMackie"}
)
public class VerifyVelocity {

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from(PlayerIdentity.CHANNEL);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final Gson gson;
    private final Map<UUID, PlayerIdentity> verifiedPlayers = new ConcurrentHashMap<>();
    private Map<String, Object> config;
    private VerifyManager verifyManager;
    /** The API client, retained so it can be closed on shutdown. */
    private VerifyClient client;
    /** The periodic re-verification task, retained so it can be cancelled on shutdown. */
    private ScheduledTask verificationTask;

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
     * Sets up the config, channel registrar, verification manager, and listeners.
     *
     * @param event the initialization event
     */
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
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

        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            this.config = yaml.load(in);
        } catch (Exception e) {
            logger.error("Could not load config, using empty map", e);
            this.config = Map.of();
        }

        proxy.getChannelRegistrar().register(IDENTIFIER);

        String apiKey = String.valueOf(config.getOrDefault("api_key", ""));
        int timeoutSeconds = ((Number) config.getOrDefault("api_timeout_seconds", 10)).intValue();
        this.client = new VerifyClient(
                String.valueOf(config.getOrDefault("api_base_url", "http://localhost")),
                apiKey,
                gson,
                logger,
                timeoutSeconds
        );
        VerifyService service = new VerifyService(client);
        this.verifyManager = new VerifyManager(service, config);

        proxy.getEventManager().register(this, new PlayerListener(this, verifiedPlayers));

        startVerificationTask();

        logger.info("Sylphian-Verify initialised successfully");
    }

    /**
     * Tears down everything registered in {@link #onProxyInitialization}: cancels
     * the periodic task, unregisters the plugin channel, clears the identity cache,
     * and closes the API client.
     *
     * @param event the proxy shutdown event
     */
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (verificationTask != null) {
            verificationTask.cancel();
            verificationTask = null;
        }
        proxy.getChannelRegistrar().unregister(IDENTIFIER);
        verifiedPlayers.clear();
        if (client != null) {
            client.close();
            client = null;
        }
    }

    /**
     * Starts a periodic task that checks the verification status of all online players.
     * If a player is no longer verified, they are kicked from the proxy.
     */
    private void startVerificationTask() {
        int interval = ((Number) config.getOrDefault("verification_interval_minutes", 10)).intValue();
        if (interval <= 0) {
            logger.info("Periodic verification task is disabled (interval set to 0)");
            return;
        }

        logger.info("Scheduling verification task to run every {} minutes", interval);

        this.verificationTask = proxy.getScheduler().buildTask(this, () -> {
                    Collection<Player> allPlayers = proxy.getAllPlayers();
                    if (allPlayers.isEmpty()) return;

                    List<UUID> uuids = allPlayers.stream()
                            .map(Player::getUniqueId)
                            .collect(Collectors.toList());

                    verifyManager.checkPeriodicBatch(uuids)
                            .thenAccept(results -> {
                                for (Player player : allPlayers) {
                                    if (!player.isActive()) continue;

                                    UUID uuid = player.getUniqueId();
                                    VerificationResult result = results.get(uuid);
                                    if (result == null) continue;

                                    if (!result.allowed()) {
                                        logger.warn("Player {} ({}) failed periodic verification. Disconnecting.", player.getUsername(), uuid);
                                        verifiedPlayers.remove(uuid);
                                        player.disconnect(result.kickMessage());
                                    } else if (result.identity() != null) {
                                        verifiedPlayers.put(uuid, result.identity());
                                    }
                                }
                            });
                })
                .delay(interval, TimeUnit.MINUTES)
                .repeat(interval, TimeUnit.MINUTES)
                .schedule();
    }

    /** @return the proxy server */
    public ProxyServer getProxy() {
        return proxy;
    }

    /** @return the Gson instance */
    public Gson getGson() {
        return gson;
    }

    /** @return the VerifyManager instance */
    public VerifyManager getVerifyManager() {
        return verifyManager;
    }

    /** @return the configuration map */
    public Map<String, Object> getConfig() {
        return config;
    }

    /** @return the logger */
    public Logger getLogger() {
        return logger;
    }
}
