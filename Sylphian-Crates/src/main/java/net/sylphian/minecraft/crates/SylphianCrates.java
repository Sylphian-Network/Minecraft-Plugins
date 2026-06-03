package net.sylphian.minecraft.crates;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.crates.api.CratesAPI;
import net.sylphian.minecraft.crates.api.CratesAPIImpl;
import net.sylphian.minecraft.crates.command.CratesCommand;
import net.sylphian.minecraft.crates.command.SylphianCratesCommand;
import net.sylphian.minecraft.crates.config.CrateConfig;
import net.sylphian.minecraft.crates.config.CrateConfigLoader;
import net.sylphian.minecraft.crates.config.KeyConfig;
import net.sylphian.minecraft.crates.config.KeyConfigLoader;
import net.sylphian.minecraft.crates.listener.CratesListener;
import net.sylphian.minecraft.crates.service.CrateService;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

/**
 * Main plugin class for Sylphian-Crates.
 *
 * <p>Loads key and crate configurations, registers the {@link CratesAPI}
 * with Bukkit's services manager so external plugins can access it,
 * and wires up all GUI event listeners.</p>
 */
public final class SylphianCrates extends JavaPlugin {

    private Map<String, KeyConfig> keys;
    private Map<String, CrateConfig> crates;

    @Override
    public void onEnable() {
        saveResource("keys.yml", false);
        saveResource("crates.yml", false);

        keys = new KeyConfigLoader(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "keys.yml")), getLogger()).loadKeys();
        crates = new CrateConfigLoader(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "crates.yml")), getLogger()).loadCrates();

        CrateService crateService = new CrateService();

        getServer().getServicesManager().register(CratesAPI.class, new CratesAPIImpl(keys, this), this, ServicePriority.Normal);

        getServer().getPluginManager().registerEvents(new CratesListener(this, crateService), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("sylphian-crates", new SylphianCratesCommand(this));
            commands.register("crates", new CratesCommand());
        });

        getLogger().info("Sylphian Crates enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Sylphian Crates disabled!");
    }

    /**
     * Returns the currently loaded key configurations.
     *
     * @return a map of key ID to KeyConfig
     */
    public Map<String, KeyConfig> getKeys() { return keys; }

    /**
     * Returns the currently loaded crate configurations.
     *
     * @return a map of crate ID to CrateConfig
     */
    public Map<String, CrateConfig> getCrates() { return crates; }

    /**
     * Reloads keys.yml and crates.yml from disk and updates all dependent services.
     *
     * @param sender the command sender to notify of success or failure, or null if called internally
     */
    public void reload(CommandSender sender) {
        try {
            keys = new KeyConfigLoader(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "keys.yml")), getLogger()).loadKeys();
            crates = new CrateConfigLoader(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "crates.yml")), getLogger()).loadCrates();

            getLogger().info("Configuration reloaded successfully.");
            if (sender != null) {
                sender.sendMessage(Component.text("Sylphian Crates reloaded successfully.", NamedTextColor.GREEN));
            }
        } catch (Exception e) {
            getLogger().severe("Failed to reload — keeping existing configuration. Error: " + e.getMessage());
            if (sender != null) {
                sender.sendMessage(Component.text("Reload failed. Check console for details.", NamedTextColor.RED));
            }
        }
    }
}