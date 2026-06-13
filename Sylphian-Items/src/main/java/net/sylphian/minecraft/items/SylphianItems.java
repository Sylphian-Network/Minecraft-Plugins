package net.sylphian.minecraft.items;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.sylphian.minecraft.items.command.SylphianItemsCommand;
import net.sylphian.minecraft.items.item.ItemRegistry;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Sylphian-Core.
 *
 * <p>Initialises the cross-plugin {@link ItemRegistry} and registers
 * the {@code /sylphian-items} administrative command.</p>
 */
public final class SylphianItems extends JavaPlugin {

    @Override
    public void onEnable() {
        ItemRegistry.init(getLogger());

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("sylphian-items", new SylphianItemsCommand());
        });

        getLogger().info("Sylphian Core enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Sylphian Core disabled!");
    }
}
