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

public final class SylphianProfile extends JavaPlugin {
    private ProfileManager profileManager;
    private VisualManager visualManager;
    private PlayerService playerService;
    private Scoreboard scoreboard;

    @Override
    public void onEnable() {
        saveDefaultConfig();

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

        ProfileListener profileListener = new ProfileListener(this, playerService);
        getServer().getPluginManager().registerEvents(profileListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, profileManager), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("playtime", "View your playtime on the server.",
                        new PlaytimeCommand(playerService, getLogger()))
        );

        getLogger().info("Sylphian-Profile initialized.");
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(player -> playerService.handleQuit(player.getUniqueId()).join());
        getLogger().info("Sylphian-Profile disabled.");
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public VisualManager getVisualManager() {
        return visualManager;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }
}
