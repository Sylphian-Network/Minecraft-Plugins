package net.sylphian.minecraft.items;

import net.sylphian.minecraft.items.command.SylphianItemsCommand;
import net.sylphian.minecraft.items.item.ItemRegistry;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Sylphian-Items.
 *
 * <p>Initialises the cross-plugin {@link ItemRegistry} and registers
 * the {@code /sylphian-items} administrative command.</p>
 */
public final class SylphianItems extends JavaPlugin {

    @Override
    public void onEnable() {
        ItemRegistry.init(getLogger());

        new SylphianItemsCommand().register();

        getLogger().info("Sylphian Items enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Sylphian Items disabled!");
    }
}
