package net.sylphian.minecraft.clans;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.sylphian.minecraft.clans.api.ClanProvider;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.cache.TerritoryCache;
import net.sylphian.minecraft.clans.command.ClanAdminCommand;
import net.sylphian.minecraft.clans.command.ClanCommand;
import net.sylphian.minecraft.clans.db.migrations.Migration001CreateClans;
import net.sylphian.minecraft.clans.db.migrations.Migration002CreateClanMembers;
import net.sylphian.minecraft.clans.db.migrations.Migration003CreateClanMemberPermissions;
import net.sylphian.minecraft.clans.db.migrations.Migration004CreateClanClaims;
import net.sylphian.minecraft.clans.db.migrations.Migration005AddClaimsFK;
import net.sylphian.minecraft.clans.db.migrations.Migration006CreateClanHomes;
import net.sylphian.minecraft.clans.db.repositories.ClanHomeRepository;
import net.sylphian.minecraft.clans.db.repositories.ClanRepository;
import net.sylphian.minecraft.clans.db.repositories.ClaimRepository;
import net.sylphian.minecraft.clans.listener.ClanListener;
import net.sylphian.minecraft.clans.listener.TerritoryNotificationListener;
import net.sylphian.minecraft.clans.listener.TerritoryProtectionListener;
import net.sylphian.minecraft.clans.model.ClanPermission;
import net.sylphian.minecraft.clans.service.ClanHomeWarmupManager;
import net.sylphian.minecraft.clans.service.ClanInviteService;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import net.sylphian.minecraft.database.DatabaseService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main plugin class for Sylphian-Clans.
 */
public final class SylphianClans extends JavaPlugin {

    private ClanService clanService;

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
                new Migration006CreateClanHomes()
        ));
        DatabaseService.runMigrations("Sylphian-Clans", getLogger());

        ClanRepository clanRepository = new ClanRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor(), serverId);
        ClaimRepository claimRepository = new ClaimRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor(), serverId);
        ClanHomeRepository homeRepository = new ClanHomeRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor());

        ClanCache clanCache = new ClanCache();
        TerritoryCache territoryCache = new TerritoryCache();

        int maxClaims = getConfig().getInt("max-claims-per-clan", 50);
        long inviteExpiry = getConfig().getLong("invite-expiry-seconds", 300);
        int homeWarmup = getConfig().getInt("home-warmup-seconds", 3);
        List<ClanPermission> defaultPerms = getConfig()
                .getStringList("default-member-permissions").stream()
                .map(ClanPermission::valueOf)
                .toList();

        TerritoryService territoryService = new TerritoryService(claimRepository, territoryCache, this, maxClaims);
        ClanInviteService inviteService = new ClanInviteService(inviteExpiry);
        clanService = new ClanService(clanRepository, homeRepository, territoryService, clanCache, this, defaultPerms);

        ClanProvider.register(clanService);

        territoryService.seedCache().exceptionally(ex -> {
            getLogger().warning("Failed to seed territory cache: " + ex.getMessage());
            return null;
        });

        ClanHomeWarmupManager warmupManager = new ClanHomeWarmupManager(this, homeWarmup);

        getServer().getPluginManager().registerEvents(new ClanListener(clanService), this);
        getServer().getPluginManager().registerEvents(new TerritoryProtectionListener(territoryService, clanCache), this);
        getServer().getPluginManager().registerEvents(new TerritoryNotificationListener(territoryService, clanService), this);
        getServer().getPluginManager().registerEvents(warmupManager, this);

        ClanCommand clanCommand = new ClanCommand(clanService, inviteService, territoryService, clanCache, warmupManager);
        ClanAdminCommand adminCommand = new ClanAdminCommand(clanService, territoryService);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("clan", "Manage your clan.", clanCommand);
            event.registrar().register("sylphian-clans", "Administrative clan commands.", adminCommand);
        });

        getLogger().info("Sylphian-Clans initialised.");
    }

    @Override
    public void onDisable() {
        ClanProvider.unregister();
        getLogger().info("Sylphian-Clans disabled.");
    }
}
