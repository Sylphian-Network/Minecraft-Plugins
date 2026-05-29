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

@Plugin(
        id = "sylphian-verify-velocity",
        name = "Sylphian-Verify-Velocity",
        version = "1.0.0",
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

    @Inject
    public VerifyVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

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

        String apiKey = (String) config.getOrDefault("api_key", "");
        VerifyClient client = new VerifyClient(
                (String) config.getOrDefault("api_base_url", "http://localhost"),
                apiKey,
                gson,
                logger
        );
        VerifyService service = new VerifyService(client);
        this.verifyManager = new VerifyManager(service, config);

        proxy.getEventManager().register(this, new PlayerListener(this, verifiedPlayers));

        startVerificationTask();

        logger.info("Sylphian-Verify-Velocity initialised successfully");
    }

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

    public ProxyServer getProxy() {
        return proxy;
    }

    public Gson getGson() {
        return gson;
    }

    public VerifyManager getVerifyManager() {
        return verifyManager;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }
}
