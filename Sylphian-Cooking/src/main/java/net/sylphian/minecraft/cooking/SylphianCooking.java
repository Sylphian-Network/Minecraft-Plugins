package net.sylphian.minecraft.cooking;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.cooking.commands.SylphianCookingCommand;
import net.sylphian.minecraft.cooking.config.FuelConfigLoader;
import net.sylphian.minecraft.cooking.config.RecipeConfigLoader;
import net.sylphian.minecraft.cooking.gui.CookingStationGui;
import net.sylphian.minecraft.cooking.item.CookingItemProvider;
import net.sylphian.minecraft.cooking.listener.CookingStationListener;
import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import net.sylphian.minecraft.cooking.skill.SkillsBridge;
import net.sylphian.minecraft.cooking.station.CookingStationService;
import net.sylphian.minecraft.items.item.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Main plugin class for Sylphian-Cooking.
 *
 * <p>Wires together config loading, the cooking station service, event listeners,
 * and the Sylphian-Items item registry on enable; tears everything down on disable.</p>
 */
public final class SylphianCooking extends JavaPlugin {

    private CookingStationService stationService;
    private CookingItemProvider itemProvider;
    private SkillsBridge skillsBridge;

    private File recipesFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getResource("recipes.yml") != null && !new File(getDataFolder(), "recipes.yml").exists()) {
            saveResource("recipes.yml", false);
            getLogger().warning("No recipes.yml found. Generating a default recipes file.");
        }

        recipesFile = new File(getDataFolder(), "recipes.yml");

        List<CookingRecipe> recipes = loadRecipes();
        Map<Material, Integer> fuels = loadFuels();

        CookingStationGui gui = new CookingStationGui();

        stationService = new CookingStationService(this, recipes, fuels, gui);
        stationService.start();

        itemProvider = new CookingItemProvider(recipes);
        ItemRegistry.register(itemProvider);

        getServer().getPluginManager().registerEvents(new CookingStationListener(this, stationService), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("sylphian-cooking", new SylphianCookingCommand(this));
        });

        if (getServer().getPluginManager().getPlugin("Sylphian-Skills") != null) {
            skillsBridge = new SkillsBridge(this);
        }

        getLogger().info("Sylphian Cooking enabled! [" + recipes.size() + " recipe(s) loaded]");
    }

    @Override
    public void onDisable() {
        if (skillsBridge != null) skillsBridge.unregister();
        if (stationService != null) stationService.shutdown();
        ItemRegistry.unregister("sylphian-cooking");
        getLogger().info("Sylphian Cooking disabled.");
    }

    /** @return the cooking station service */
    public CookingStationService getStationService() {
        return stationService;
    }

    private List<CookingRecipe> loadRecipes() {
        FileConfiguration recipesConfig = YamlConfiguration.loadConfiguration(recipesFile);
        return new RecipeConfigLoader(recipesConfig, getLogger()).loadRecipes();
    }

    private Map<Material, Integer> loadFuels() {
        return new FuelConfigLoader(getConfig(), getLogger()).loadFuels();
    }

    /**
     * Reloads {@code config.yml} and {@code recipes.yml} from disk and pushes updated
     * data into the running service without restarting the plugin.
     *
     * @param sender the command sender to notify of success or failure, or null if called internally
     */
    public void reload(CommandSender sender) {
        try {
            reloadConfig();
            List<CookingRecipe> recipes = new RecipeConfigLoader(
                    YamlConfiguration.loadConfiguration(new File(getDataFolder(), "recipes.yml")),
                    getLogger()).loadRecipes();
            Map<Material, Integer> fuels = new FuelConfigLoader(
                    getConfig(), getLogger()).loadFuels();

            stationService.reload(recipes, fuels);
            if (skillsBridge != null) skillsBridge.reload();

            getLogger().info("Configuration reloaded successfully.");
            if (sender != null)
                sender.sendMessage(Component.text("Sylphian Cooking reloaded successfully.", NamedTextColor.GREEN));
        } catch (Exception e) {
            getLogger().severe("Failed to reload — keeping existing configuration. Error: " + e.getMessage());
            if (sender != null)
                sender.sendMessage(Component.text("Reload failed. Check console for details.", NamedTextColor.RED));
        }
    }
}
