package net.sylphian.minecraft.clans;

import net.sylphian.minecraft.clans.api.ClanProvider;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.cache.TerritoryCache;
import net.sylphian.minecraft.clans.command.ClanAdminCommand;
import net.sylphian.minecraft.clans.command.ClanCommand;
import net.sylphian.minecraft.clans.config.ClansConfig;
import net.sylphian.minecraft.clans.db.migrations.Migration001CreateClans;
import net.sylphian.minecraft.clans.db.migrations.Migration002CreateClanMembers;
import net.sylphian.minecraft.clans.db.migrations.Migration003CreateClanMemberPermissions;
import net.sylphian.minecraft.clans.db.migrations.Migration004CreateClanClaims;
import net.sylphian.minecraft.clans.db.migrations.Migration005AddClaimsFK;
import net.sylphian.minecraft.clans.db.migrations.Migration006CreateClanWarps;
import net.sylphian.minecraft.clans.db.repositories.ClanRepository;
import net.sylphian.minecraft.clans.db.repositories.ClanWarpRepository;
import net.sylphian.minecraft.clans.db.repositories.ClaimRepository;
import net.sylphian.minecraft.clans.gui.ClanPermissionMenu;
import net.sylphian.minecraft.clans.gui.ClanWarpAccessMenu;
import net.sylphian.minecraft.clans.gui.ClanWarpMenu;
import net.sylphian.minecraft.clans.listener.ClanListener;
import net.sylphian.minecraft.clans.listener.ClanPermissionListener;
import net.sylphian.minecraft.clans.listener.ClanWarpListener;
import net.sylphian.minecraft.clans.listener.TerritoryProtectionListener;
import net.sylphian.minecraft.clans.listener.TerritoryTitleListener;
import net.sylphian.minecraft.clans.listener.TerritoryTrackingListener;
import net.sylphian.minecraft.clans.placeholder.ClanPlaceholderExpansion;
import net.sylphian.minecraft.clans.service.ClanTeleportWarmupManager;
import net.sylphian.minecraft.clans.service.ClanInviteService;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.ClanWarpService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import net.sylphian.minecraft.database.DatabaseService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main plugin class for Sylphian-Clans.
 */
public final class SylphianClans extends JavaPlugin {

    private ClanService clanService;
    private TerritoryService territoryService;
    private ClanWarpService warpService;
    private ClanInviteService inviteService;
    private ClanTeleportWarmupManager warmupManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String serverId = getConfig().getString("server-id", "default");
        if (serverId.isBlank() || serverId.equalsIgnoreCase("default")) {
            getLogger().severe("server-id is not configured. Set a unique 'server-id' in config.yml for this server instance, then restart. Disabling Sylphian-Clans to avoid sharing clan data across servers.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Server ID: " + serverId);

        DatabaseService.registerMigrations(List.of(
                new Migration001CreateClans(),
                new Migration002CreateClanMembers(),
                new Migration003CreateClanMemberPermissions(),
                new Migration004CreateClanClaims(),
                new Migration005AddClaimsFK(),
                new Migration006CreateClanWarps()
        ));
        DatabaseService.runMigrations("Sylphian-Clans", getLogger());

        ClanRepository clanRepository = new ClanRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor(), serverId);
        ClaimRepository claimRepository = new ClaimRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor(), serverId);
        ClanWarpRepository warpRepository = new ClanWarpRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor());

        ClanCache clanCache = new ClanCache();
        TerritoryCache territoryCache = new TerritoryCache();

        ClansConfig config = ClansConfig.from(getConfig(), getLogger());

        territoryService = new TerritoryService(claimRepository, territoryCache, this, config.maxClaimsPerClan());
        inviteService = new ClanInviteService(config.inviteExpiry());
        warpService = new ClanWarpService(warpRepository, config.maxWarpsPerClan(), this);
        clanService = new ClanService(clanRepository, territoryService, clanCache, this, config.defaultMemberPerms());

        ClanProvider.register(clanService);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanPlaceholderExpansion().register();
        }

        territoryService.seedCache().exceptionally(ex -> {
            getLogger().warning("Failed to seed territory cache: " + ex.getMessage());
            return null;
        });

        warmupManager = new ClanTeleportWarmupManager(this, config.teleportWarmup());
        ClanPermissionMenu permissionMenu = new ClanPermissionMenu(clanService, clanCache, this);
        ClanWarpAccessMenu warpAccessMenu = new ClanWarpAccessMenu(warpService, clanCache, this);
        ClanWarpMenu warpMenu = new ClanWarpMenu(warpService, clanCache, warmupManager, warpAccessMenu, this);

        getServer().getPluginManager().registerEvents(new ClanListener(clanService), this);
        getServer().getPluginManager().registerEvents(new TerritoryProtectionListener(territoryService, clanCache), this);
        getServer().getPluginManager().registerEvents(new TerritoryTrackingListener(territoryService), this);
        getServer().getPluginManager().registerEvents(new TerritoryTitleListener(territoryService, clanService), this);
        getServer().getPluginManager().registerEvents(new ClanPermissionListener(), this);
        getServer().getPluginManager().registerEvents(new ClanWarpListener(), this);
        getServer().getPluginManager().registerEvents(warmupManager, this);

        ClanCommand clanCommand = new ClanCommand(clanService, inviteService, territoryService, clanCache,
                warmupManager, permissionMenu, warpService, warpMenu);
        clanCommand.register();

        ClanAdminCommand adminCommand = new ClanAdminCommand(clanService, territoryService, this);
        adminCommand.register();

        getLogger().info("Sylphian-Clans initialised.");
    }

    /**
     * Re-reads {@code config.yml} and applies the new settings to the live services.
     * On any failure the previous configuration is kept and the error is reported.
     *
     * @param sender the command sender to notify, or {@code null} for a silent reload
     */
    public void reload(CommandSender sender) {
        try {
            reloadConfig();
            ClansConfig fresh = ClansConfig.from(getConfig(), getLogger());
            territoryService.setMaxClaimsPerClan(fresh.maxClaimsPerClan());
            warpService.setMaxWarpsPerClan(fresh.maxWarpsPerClan());
            clanService.setDefaultMemberPermissions(fresh.defaultMemberPerms());
            inviteService.setExpirySeconds(fresh.inviteExpiry());
            warmupManager.setWarmupSeconds(fresh.teleportWarmup());
            getLogger().info("Configuration reloaded.");
            if (sender != null) sender.sendMessage(Component.text("Clans config reloaded.", NamedTextColor.GREEN));
        } catch (Exception e) {
            getLogger().severe("Reload failed, keeping old config: " + e.getMessage());
            if (sender != null) sender.sendMessage(Component.text("Reload failed — see console.", NamedTextColor.RED));
        }
    }

    @Override
    public void onDisable() {
        ClanProvider.unregister();
        getLogger().info("Sylphian-Clans disabled.");
    }
}
