package net.sylphian.minecraft.core;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.sylphian.minecraft.core.command.SylphianCoreCommand;
import net.sylphian.minecraft.core.item.ItemRegistry;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Sylphian-Core.
 *
 * <p>Initialises the cross-plugin {@link ItemRegistry} and registers
 * the {@code /sylphian-core} administrative command.</p>
 */
public final class SylphianCore extends JavaPlugin {

    @Override
    public void onEnable() {
        ItemRegistry.init(getLogger());

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("sylphian-core", new SylphianCoreCommand());
        });

        getLogger().info("Sylphian Core enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Sylphian Core disabled!");
    }
}
