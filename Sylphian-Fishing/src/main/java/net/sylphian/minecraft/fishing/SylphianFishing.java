package net.sylphian.minecraft.fishing;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.fishing.commands.EncyclopaediaCommand;
import net.sylphian.minecraft.fishing.config.FishConfigLoader;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.db.migrations.Migration003CreateFishEncyclopaedia;
import net.sylphian.minecraft.fishing.db.repositories.FishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.fish.FishEntry;
import net.sylphian.minecraft.fishing.gui.EncyclopaediaMenu;
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

public class SylphianFishing extends JavaPlugin {

    private LootManager lootManager;
    private FishMutationService mutationService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("fish.yml", false);

        DatabaseService.registerMigrations(List.of(
                new Migration003CreateFishEncyclopaedia()
        ));
        DatabaseService.runMigrations(getLogger());

        ConfigLoader configLoader = new ConfigLoader(getConfig());

        this.mutationService = new FishMutationService(configLoader);
        this.mutationService.registerMutation("super_fish", new SuperFishMutation());

        File fishFile = new File(getDataFolder(), "fish.yml");
        FileConfiguration fishConfig = YamlConfiguration.loadConfiguration(fishFile);
        FishConfigLoader loader = new FishConfigLoader(fishConfig);
        List<FishEntry> fish = loader.loadFish();

        this.lootManager = new LootManager(fish);

        FishEncyclopaediaRepository encyclopaediaRepository = new FishEncyclopaediaRepository(
                DatabaseService.getJdbi(),
                DatabaseService.getExecutor()
        );

        getServer().getPluginManager().registerEvents(new FishingListener(lootManager, mutationService, encyclopaediaRepository, this), this);
        getServer().getPluginManager().registerEvents(new EncyclopaediaListener(), this);
        getServer().getPluginManager().registerEvents(new SuperFishEnchantmentListener(configLoader), this);

        EncyclopaediaMenu menu = new EncyclopaediaMenu(fish, encyclopaediaRepository, this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register("encyclopaedia", new EncyclopaediaCommand(menu));
        });

        getLogger().info("Sylphian Fishing enabled!");
    }

    @Override
    public void onDisable() {
        Rarity.clear();
        getLogger().info("Sylphian Fishing disabled!");
    }
}