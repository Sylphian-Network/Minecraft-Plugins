package net.sylphian.minecraft.fishing;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.fishing.commands.EncyclopaediaCommand;
import net.sylphian.minecraft.fishing.commands.SylphianFishingCommand;
import net.sylphian.minecraft.fishing.config.*;
import net.sylphian.minecraft.fishing.db.migrations.Migration001CreateFishEncyclopaedia;
import net.sylphian.minecraft.fishing.db.repositories.FishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.listeners.BaitListener;
import net.sylphian.minecraft.fishing.services.*;
import net.sylphian.minecraft.fishing.fish.LootEntry;
import net.sylphian.minecraft.fishing.gui.EncyclopaediaMenu;
import net.sylphian.minecraft.fishing.listeners.EncyclopaediaListener;
import net.sylphian.minecraft.fishing.listeners.FishingListener;
import net.sylphian.minecraft.fishing.listeners.SuperFishEnchantmentListener;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.services.mutation.impl.SuperFishMutation;
import net.sylphian.minecraft.fishing.sidebar.FishingContributor;
import net.sylphian.minecraft.scoreboard.services.SidebarService;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Main plugin class for Sylphian-Fishing.
 * Responsible for initializing core services (loot, mutations, database),
 * registering listeners and commands, and managing the plugin lifecycle.
 */
public class SylphianFishing extends JavaPlugin {

    private EncyclopaediaMenu encyclopaediaMenu;

    private LootService lootService;
    private FishMutationService mutationService;
    private CatchEffectService catchEffectService;
    private BiteTimerService biteTimerService;
    private BaitZoneService baitZoneService;

    private BaitListener baitListener;

    private File lootTableFile;
    private File baitFile;

    /**
     * Initializes the plugin, including configuration loading, database migrations,
     * service registration, and command/event setup.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("loot_table.yml", false);
        saveResource("baits.yml", false);

        // Register database migrations for the fishing encyclopaedia
        DatabaseService.registerMigrations(List.of(
                new Migration001CreateFishEncyclopaedia()
        ));
        DatabaseService.runMigrations("Sylphian-Fishing", getLogger());

        ConfigLoader configLoader = new ConfigLoader(getConfig(), getLogger());

        this.lootTableFile = new File(getDataFolder(), "loot_table.yml");
        FileConfiguration lootTableFile = YamlConfiguration.loadConfiguration(this.lootTableFile);
        List<LootEntry> lootTableEntries = new LootTableConfigLoader(lootTableFile, getLogger()).loadEntries();

        this.baitFile = new File(getDataFolder(), "baits.yml");
        FileConfiguration baitConfig = YamlConfiguration.loadConfiguration(baitFile);
        Map<String, BaitConfig> baits = new BaitConfigLoader(baitConfig, getLogger()).loadBaits();

        this.mutationService = new FishMutationService(configLoader);
        this.mutationService.registerMutation("super_fish", new SuperFishMutation());
        this.baitZoneService = new BaitZoneService(baits);
        this.baitZoneService.start(this);
        this.catchEffectService = new CatchEffectService(configLoader, getLogger());
        this.lootService = new LootService(lootTableEntries, configLoader);
        this.biteTimerService = new BiteTimerService(configLoader, lootService, baitZoneService, getLogger());

        this.baitListener = new BaitListener(baitZoneService, this);
        baitListener.start(this);

        FishingContributor baitContributor = new FishingContributor(baitZoneService);
        SidebarService.registerContributor(baitContributor);

        // Initialize repository with JDBI and async executor from Sylphian-Database
        FishEncyclopaediaRepository encyclopaediaRepository = new FishEncyclopaediaRepository(
                DatabaseService.getJdbi(),
                DatabaseService.getExecutor()
        );

        getServer().getPluginManager().registerEvents(new FishingListener(lootService, mutationService, catchEffectService, biteTimerService, baitZoneService, baitContributor, encyclopaediaRepository, this), this);
        getServer().getPluginManager().registerEvents(baitListener, this);
        getServer().getPluginManager().registerEvents(new EncyclopaediaListener(), this);
        getServer().getPluginManager().registerEvents(new SuperFishEnchantmentListener(mutationService), this);

        this.encyclopaediaMenu = new EncyclopaediaMenu(lootTableEntries, encyclopaediaRepository, this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register("sylphian-fishing", new SylphianFishingCommand(this, catchEffectService, lootService, mutationService, baitZoneService));
            commands.register("encyclopaedia", new EncyclopaediaCommand(encyclopaediaMenu));
        });

        getLogger().info("Sylphian Fishing enabled!");
    }

    /**
     * Cleans up resources when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        baitZoneService.shutdown();
        baitListener.shutdown();
        Rarity.clear();
        getLogger().info("Sylphian Fishing disabled!");
    }

    /**
     * Reloads config.yml and loot_table.yml from disk and pushes the updated
     * configuration into all dependent services without a restart.
     * If parsing fails, the existing configuration is kept and the error
     * is logged to console.
     *
     * @param sender the command sender to notify of success or failure,
     *               or null if called internally
     */
    public void reload(CommandSender sender) {
        try {
            reloadConfig();
            FileConfiguration fishConfig = YamlConfiguration.loadConfiguration(lootTableFile);
            FileConfiguration baitConfig = YamlConfiguration.loadConfiguration(baitFile);

            ConfigLoader newConfig = new ConfigLoader(getConfig(), getLogger());
            List<LootEntry> newLootTableEntries = new LootTableConfigLoader(fishConfig, getLogger()).loadEntries();
            Map<String, BaitConfig> newBaits = new BaitConfigLoader(baitConfig, getLogger()).loadBaits();

            lootService.reload(newConfig, newLootTableEntries);
            catchEffectService.reload(newConfig);
            mutationService.reload(newConfig);
            biteTimerService.reload(newConfig);
            baitZoneService.reload(newBaits);

            encyclopaediaMenu.reload(newLootTableEntries);

            getLogger().info("Configuration reloaded successfully.");
            if (sender != null) {
                sender.sendMessage(Component.text("Configuration reloaded successfully.", NamedTextColor.GREEN));
            }
        } catch (Exception e) {
            getLogger().severe("Failed to reload — keeping existing configuration. Error: " + e.getMessage());
            if (sender != null) {
                sender.sendMessage(Component.text("Reload failed. Check console for details.", NamedTextColor.RED));
            }
        }
    }
}