package net.sylphian.minecraft.fishing;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.fishing.commands.EncyclopaediaCommand;
import net.sylphian.minecraft.fishing.commands.TestEffectCommand;
import net.sylphian.minecraft.fishing.commands.TestFishingCommand;
import net.sylphian.minecraft.fishing.config.FishConfigLoader;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.db.migrations.Migration001CreateFishEncyclopaedia;
import net.sylphian.minecraft.fishing.db.repositories.FishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.effects.CatchEffectService;
import net.sylphian.minecraft.fishing.fish.FishEntry;
import net.sylphian.minecraft.fishing.gui.EncyclopaediaMenu;
import net.sylphian.minecraft.fishing.listeners.BiteTimerListener;
import net.sylphian.minecraft.fishing.listeners.EncyclopaediaListener;
import net.sylphian.minecraft.fishing.listeners.FishingListener;
import net.sylphian.minecraft.fishing.listeners.SuperFishEnchantmentListener;
import net.sylphian.minecraft.fishing.loot.LootManager;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.mutation.FishMutationService;
import net.sylphian.minecraft.fishing.mutation.impl.SuperFishMutation;
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

    private LootManager lootManager;

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

        FishMutationService mutationService = new FishMutationService(configLoader);
        mutationService.registerMutation("super_fish", new SuperFishMutation());

        CatchEffectService catchEffectService = new CatchEffectService(configLoader, getLogger());

        // Load fish from fish.yml
        File fishFile = new File(getDataFolder(), "fish.yml");
        FileConfiguration fishConfig = YamlConfiguration.loadConfiguration(fishFile);
        FishConfigLoader loader = new FishConfigLoader(fishConfig, getLogger());
        List<FishEntry> fish = loader.loadFish();

        this.lootManager = new LootManager(fish, configLoader);

        // Initialize repository with JDBI and async executor from Sylphian-Database
        FishEncyclopaediaRepository encyclopaediaRepository = new FishEncyclopaediaRepository(
                DatabaseService.getJdbi(),
                DatabaseService.getExecutor()
        );

        getServer().getPluginManager().registerEvents(new FishingListener(lootManager, mutationService, catchEffectService, encyclopaediaRepository, this), this);
        getServer().getPluginManager().registerEvents(new BiteTimerListener(configLoader, lootManager, this), this);
        getServer().getPluginManager().registerEvents(new EncyclopaediaListener(), this);
        getServer().getPluginManager().registerEvents(new SuperFishEnchantmentListener(configLoader), this);

        EncyclopaediaMenu menu = new EncyclopaediaMenu(fish, encyclopaediaRepository, this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register("encyclopaedia", new EncyclopaediaCommand(menu));
            commands.register("test_fishing", new TestFishingCommand(lootManager, configLoader));
            commands.register("test_effect", new TestEffectCommand(catchEffectService));
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
}