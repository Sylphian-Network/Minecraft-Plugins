package net.sylphian.minecraft.profile;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.profile.command.PlaytimeCommand;
import net.sylphian.minecraft.profile.db.migrations.Migration001CreatePlayers;
import net.sylphian.minecraft.profile.db.migrations.Migration002CreateSessions;
import net.sylphian.minecraft.profile.db.repositories.PlayerRepository;
import net.sylphian.minecraft.profile.db.repositories.SessionRepository;
import net.sylphian.minecraft.profile.listener.ChatListener;
import net.sylphian.minecraft.profile.listener.ProfileListener;
import net.sylphian.minecraft.profile.service.PlayerService;
import net.sylphian.minecraft.profile.utils.ProfileManager;
import net.sylphian.minecraft.profile.utils.VisualManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

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
    private Scoreboard scoreboard;

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
        this.playerService = new PlayerService(
                new PlayerRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor()),
                new SessionRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor()),
                profileManager
        );
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        // Register event listeners
        ProfileListener profileListener = new ProfileListener(this, playerService);
        getServer().getPluginManager().registerEvents(profileListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, profileManager), this);

        // Register commands using Paper's command lifecycle
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("playtime", "View your playtime on the server.",
                        new PlaytimeCommand(playerService, getLogger()))
        );

        getLogger().info("Sylphian-Profile initialized.");
    }

    /**
     * Called when the plugin is disabled.
     * Ensures all active player sessions are saved to the database before the plugin shuts down.
     */
    @Override
    public void onDisable() {
        // Handle logout for all online players to ensure session data is saved
        Bukkit.getOnlinePlayers().forEach(player -> playerService.handleQuit(player.getUniqueId()).join());
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

    /**
     * Gets the global scoreboard used for teams and visual styling.
     * @return the scoreboard
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }
}
