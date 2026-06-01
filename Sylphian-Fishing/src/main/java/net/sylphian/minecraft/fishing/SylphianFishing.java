package net.sylphian.minecraft.fishing;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.fishing.commands.EncyclopaediaCommand;
import net.sylphian.minecraft.fishing.commands.SylphianFishingCommand;
import net.sylphian.minecraft.fishing.commands.TestEffectCommand;
import net.sylphian.minecraft.fishing.commands.TestFishingCommand;
import net.sylphian.minecraft.fishing.config.FishConfigLoader;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.db.migrations.Migration001CreateFishEncyclopaedia;
import net.sylphian.minecraft.fishing.db.repositories.FishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.services.BiteTimerService;
import net.sylphian.minecraft.fishing.services.CatchEffectService;
import net.sylphian.minecraft.fishing.fish.FishEntry;
import net.sylphian.minecraft.fishing.gui.EncyclopaediaMenu;
import net.sylphian.minecraft.fishing.listeners.EncyclopaediaListener;
import net.sylphian.minecraft.fishing.listeners.FishingListener;
import net.sylphian.minecraft.fishing.listeners.SuperFishEnchantmentListener;
import net.sylphian.minecraft.fishing.services.LootService;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.services.FishMutationService;
import net.sylphian.minecraft.fishing.services.mutation.impl.SuperFishMutation;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

/**
 * Main plugin class for Sylphian-Fishing.
 * Responsible for initializing core services (loot, mutations, database),
 * registering listeners and commands, and managing the plugin lifecycle.
 */
public class SylphianFishing extends JavaPlugin {

    private LootService lootService;
    private FishMutationService mutationService;
    private CatchEffectService catchEffectService;
    private BiteTimerService biteTimerService;
    private File fishFile;

    /**
     * Initializes the plugin, including configuration loading, database migrations,
     * service registration, and command/event setup.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("fish.yml", false);

        // Register database migrations for the fishing encyclopaedia
        DatabaseService.registerMigrations(List.of(
                new Migration001CreateFishEncyclopaedia()
        ));
        DatabaseService.runMigrations("Sylphian-Fishing", getLogger());

        ConfigLoader configLoader = new ConfigLoader(getConfig(), getLogger());

        this.fishFile = new File(getDataFolder(), "fish.yml");
        FileConfiguration fishConfig = YamlConfiguration.loadConfiguration(fishFile);
        List<FishEntry> fish = new FishConfigLoader(fishConfig, getLogger()).loadFish();

        this.mutationService = new FishMutationService(configLoader);
        this.mutationService.registerMutation("super_fish", new SuperFishMutation());

        this.catchEffectService = new CatchEffectService(configLoader, getLogger());
        this.lootService = new LootService(fish, configLoader);
        this.biteTimerService = new BiteTimerService(configLoader, lootService, getLogger());

        // Initialize repository with JDBI and async executor from Sylphian-Database
        FishEncyclopaediaRepository encyclopaediaRepository = new FishEncyclopaediaRepository(
                DatabaseService.getJdbi(),
                DatabaseService.getExecutor()
        );

        getServer().getPluginManager().registerEvents(new FishingListener(lootService, mutationService, catchEffectService, biteTimerService, encyclopaediaRepository, this), this);
        getServer().getPluginManager().registerEvents(new EncyclopaediaListener(), this);
        getServer().getPluginManager().registerEvents(new SuperFishEnchantmentListener(mutationService), this);

        EncyclopaediaMenu menu = new EncyclopaediaMenu(fish, encyclopaediaRepository, this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register("sylphian-fishing", new SylphianFishingCommand(this));
            commands.register("encyclopaedia", new EncyclopaediaCommand(menu));
            commands.register("test_fishing", new TestFishingCommand(lootService, configLoader));
            commands.register("test_effect", new TestEffectCommand(this.catchEffectService));
        });

        getLogger().info("Sylphian Fishing enabled!");
    }

    /**
     * Cleans up resources when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        Rarity.clear();
        getLogger().info("Sylphian Fishing disabled!");
    }

    /**
     * Reloads config.yml and fish.yml from disk and pushes the updated
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
            FileConfiguration fishConfig = YamlConfiguration.loadConfiguration(fishFile);

            ConfigLoader newConfig = new ConfigLoader(getConfig(), getLogger());
            List<FishEntry> newFish = new FishConfigLoader(fishConfig, getLogger()).loadFish();

            lootService.reload(newConfig, newFish);
            catchEffectService.reload(newConfig);
            mutationService.reload(newConfig);
            biteTimerService.reload(newConfig);

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