package net.sylphian.minecraft.dimensions;

import net.sylphian.minecraft.dimensions.api.DimensionProvider;
import net.sylphian.minecraft.dimensions.command.DimensionCommand;
import net.sylphian.minecraft.dimensions.command.HubCommand;
import net.sylphian.minecraft.dimensions.command.SylphianDimensionsCommand;
import net.sylphian.minecraft.dimensions.config.DimensionsConfig;
import net.sylphian.minecraft.dimensions.listener.ClanClaimListener;
import net.sylphian.minecraft.dimensions.listener.DimensionDeathListener;
import net.sylphian.minecraft.dimensions.listener.DimensionProtectionListener;
import net.sylphian.minecraft.dimensions.listener.NaturalSpawnListener;
import net.sylphian.minecraft.dimensions.listener.PlayerConnectionListener;
import net.sylphian.minecraft.dimensions.spawn.DimensionSpawner;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import net.sylphian.minecraft.dimensions.world.TemplateManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main plugin class for Sylphian-Dimensions.
 *
 * <p>Loads dimension worlds from templates, registers the {@link DimensionProvider}
 * API, and enforces per-dimension rulesets.</p>
 */
public final class SylphianDimensions extends JavaPlugin {

    private DimensionManager dimensionManager;
    private @Nullable DimensionSpawner dimensionSpawner;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Path templatesDir = getDataFolder().toPath().resolve("templates");
        try {
            Files.createDirectories(templatesDir);
        } catch (IOException e) {
            getLogger().severe("Could not create the templates folder: " + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DimensionsConfig config = DimensionsConfig.from(getConfig(), getLogger());
        if (!config.dimensions().containsKey(config.hubName())) {
            getLogger().severe("Hub dimension '" + config.hubName() + "' is not defined in config.yml; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Unified world storage (Paper 26.1+): dimensions are keyed sylphian:<name>,
        // so their data lives under `world/dimensions/sylphian/<name>`
        TemplateManager templates = new TemplateManager(
                templatesDir,
                getServer().getWorldContainer().toPath()
                        .resolve(getServer().getWorlds().getFirst().getName())
                        .resolve("dimensions")
                        .resolve(DimensionManager.NAMESPACE),
                getLogger());
        dimensionManager = new DimensionManager(config, templates, getLogger());

        if (!dimensionManager.loadAll()) {
            getLogger().severe("Hub dimension failed to load; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DimensionProvider.register(dimensionManager);

        getServer().getPluginManager().registerEvents(new DimensionProtectionListener(dimensionManager), this);
        getServer().getPluginManager().registerEvents(new DimensionDeathListener(dimensionManager), this);
        getServer().getPluginManager().registerEvents(new NaturalSpawnListener(dimensionManager), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(dimensionManager), this);

        if (getServer().getPluginManager().getPlugin("Sylphian-Clans") != null) {
            getServer().getPluginManager().registerEvents(new ClanClaimListener(dimensionManager), this);
            getLogger().info("Sylphian-Clans detected; the claiming rule is active.");
        }

        if (getServer().getPluginManager().getPlugin("Sylphian-Entities") != null) {
            dimensionSpawner = new DimensionSpawner(this, dimensionManager);
            dimensionSpawner.start();
            getLogger().info("Sylphian-Entities detected; custom dimension spawning is active.");
        } else {
            getLogger().info("Sylphian-Entities not present; dimension spawn tables are inactive.");
        }

        new DimensionCommand(dimensionManager).register();
        new HubCommand(dimensionManager).register();
        new SylphianDimensionsCommand(this, dimensionManager, templates).register();

        getLogger().info("Sylphian Dimensions enabled!");
    }

    /**
     * Rebuilds the configuration from disk and swaps it in. A bad edit keeps
     * the old config and reports the failure to the sender.
     *
     * @param sender the command sender to notify of the outcome
     * @return true if the new configuration was applied
     */
    public boolean reload(CommandSender sender) {
        try {
            reloadConfig();
            DimensionsConfig newConfig = DimensionsConfig.from(getConfig(), getLogger());
            if (!newConfig.dimensions().containsKey(newConfig.hubName())) {
                throw new IllegalStateException("Hub dimension '" + newConfig.hubName() + "' is not defined.");
            }
            dimensionManager.reload(newConfig);
            if (dimensionSpawner != null) dimensionSpawner.reload();
            sender.sendMessage(SylphianDimensionsCommand.MINI.deserialize("<green>Sylphian-Dimensions configuration reloaded."));
            return true;
        } catch (Exception e) {
            getLogger().severe("Reload failed, keeping previous configuration: " + e.getMessage());
            sender.sendMessage(SylphianDimensionsCommand.MINI.deserialize("<red>Reload failed, keeping previous configuration: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public void onDisable() {
        if (dimensionSpawner != null) dimensionSpawner.stop();
        DimensionProvider.unregister();
        if (dimensionManager != null) dimensionManager.unloadAll();
        getLogger().info("Sylphian Dimensions disabled!");
    }
}
