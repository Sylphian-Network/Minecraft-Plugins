package net.sylphian.minecraft.profile;

import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.profile.command.PlaytimeCommand;
import net.sylphian.minecraft.profile.db.migrations.Migration001CreatePlayers;
import net.sylphian.minecraft.profile.db.migrations.Migration002CreateSessions;
import net.sylphian.minecraft.profile.db.models.PlayerModel;
import net.sylphian.minecraft.profile.db.repositories.PlayerRepository;
import net.sylphian.minecraft.profile.db.repositories.SessionRepository;
import net.sylphian.minecraft.profile.listener.ChatListener;
import net.sylphian.minecraft.profile.listener.ProfileListener;
import net.sylphian.minecraft.profile.api.ProfileProvider;
import net.sylphian.minecraft.profile.placeholder.PapiPlaceholderBridge;
import net.sylphian.minecraft.profile.placeholder.PlaceholderResolver;
import net.sylphian.minecraft.profile.service.PlayerService;
import net.sylphian.minecraft.profile.sidebar.ProfileContributor;
import net.sylphian.minecraft.profile.utils.ProfileManager;
import net.sylphian.minecraft.profile.utils.VisualManager;
import net.sylphian.minecraft.scoreboard.services.SidebarService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main plugin class for Sylphian-Profile.
 * Manages player profiles, session tracking, and visual elements like scoreboards and prefixes.
 * Coordinates between the database repositories, services, and event listeners to ensure
 * player data is correctly loaded and saved.
 */
public final class SylphianProfile extends JavaPlugin {
    private ProfileManager profileManager;
    private VisualManager visualManager;
    private PlayerService playerService;
    private PlayerRepository playerRepository;

    /**
     * Called when the plugin is enabled.
     * Initializes the database connection, registers migrations, sets up managers,
     * and registers event listeners and commands.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Register migrations for player and session tables
        DatabaseService.registerMigrations(List.of(
                new Migration001CreatePlayers(),
                new Migration002CreateSessions()
        ));
        DatabaseService.runMigrations("Sylphian-Profile", getLogger());
        
        this.profileManager = new ProfileManager();
        this.visualManager = new VisualManager(this);
        this.playerRepository = new PlayerRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor());
        this.playerService = new PlayerService(
                playerRepository,
                new SessionRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor()),
                profileManager
        );

        ProfileProvider.register((uuid, xfUserId, mcUsername, forumUsername) -> {
            long now = Instant.now().getEpochSecond();
            return playerRepository.findByUuid(uuid).thenCompose(opt -> {
                if (opt.isPresent()) {
                    PlayerModel existing = opt.get();
                    PlayerModel updated = new PlayerModel(
                            uuid, xfUserId, mcUsername, forumUsername,
                            existing.firstJoined(), now, existing.playtime(), existing.isOnline());
                    return playerRepository.update(updated);
                } else {
                    PlayerModel inserted = new PlayerModel(
                            uuid, xfUserId, mcUsername, forumUsername,
                            now, now, 0, false);
                    return playerRepository.insert(inserted);
                }
            });
        });

        // Register event listeners
        ProfileListener profileListener = new ProfileListener(this, playerService);
        getServer().getPluginManager().registerEvents(profileListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, profileManager), this);

        // Register commands using the CommandAPI
        new PlaytimeCommand(playerService, this).register();

        PlaceholderResolver resolver = null;
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            resolver = new PapiPlaceholderBridge();
        }
        SidebarService.registerContributor(new ProfileContributor(profileManager, resolver));

        getLogger().info("Sylphian-Profile initialized.");
    }

    /**
     * Called when the plugin is disabled.
     * Ensures all active player sessions are saved to the database before the plugin shuts down.
     */
    @Override
    public void onDisable() {
        List<CompletableFuture<Void>> futures = Bukkit.getOnlinePlayers().stream()
                .map(p -> playerService.handleQuit(p.getUniqueId()))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        ProfileProvider.unregister();
        getLogger().info("Sylphian-Profile disabled.");
    }

    /**
     * Gets the profile manager instance.
     * @return the profile manager
     */
    public ProfileManager getProfileManager() {
        return profileManager;
    }

    /**
     * Gets the visual manager instance.
     * @return the visual manager
     */
    public VisualManager getVisualManager() {
        return visualManager;
    }
}
