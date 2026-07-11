package net.sylphian.minecraft.entities;

import net.kyori.adventure.text.Component;
import net.sylphian.minecraft.entities.command.SylphianEntitiesCommand;
import net.sylphian.minecraft.entities.config.EntitiesConfig;
import net.sylphian.minecraft.entities.entity.ConfiguredEntityProvider;
import net.sylphian.minecraft.entities.entity.EntityRegistry;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Sylphian-Entities.
 *
 * <p>Initialises the cross-plugin {@link EntityRegistry}, parses the entity
 * definitions from {@code config.yml} into a {@link ConfiguredEntityProvider},
 * and registers the {@code /sylphian-entities} administrative command.</p>
 */
public final class SylphianEntities extends JavaPlugin {

    private ConfiguredEntityProvider provider;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EntityRegistry.init(getLogger());

        EntitiesConfig config;
        try {
            config = EntitiesConfig.from(getConfig(), getLogger());
        } catch (Exception e) {
            getLogger().severe("Failed to parse entity definitions, disabling plugin: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        provider = new ConfiguredEntityProvider(config);
        EntityRegistry.register(provider);

        new SylphianEntitiesCommand(this).register();

        getLogger().info("Sylphian Entities enabled!");
    }

    /**
     * Rebuilds the entity definitions from disk and swaps them in. A bad edit
     * keeps the old definitions and reports the failure to the sender.
     *
     * @param sender the command sender to notify of the outcome
     * @return true if the new configuration was applied
     */
    public boolean reload(CommandSender sender) {
        try {
            reloadConfig();
            provider.reload(EntitiesConfig.from(getConfig(), getLogger()));
            sender.sendMessage(SylphianEntitiesCommand.MINI.deserialize("<green>Sylphian-Entities configuration reloaded."));
            return true;
        } catch (Exception e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            getLogger().severe("Reload failed, keeping previous configuration: " + reason);
            sender.sendMessage(SylphianEntitiesCommand.MINI
                    .deserialize("<red>Reload failed, keeping previous configuration: ")
                    .append(Component.text(reason)));
            return false;
        }
    }

    @Override
    public void onDisable() {
        EntityRegistry.unregister(ConfiguredEntityProvider.NAMESPACE);
        EntityRegistry.shutdown();
        getLogger().info("Sylphian Entities disabled!");
    }
}
