package net.sylphian.minecraft.crates.economy;

import net.sylphian.minecraft.crates.SylphianCrates;
import net.sylphian.minecraft.economy.event.EconomyConfigReloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Reparses crate configs when the economy config changes, so money-reward
 * display names regenerate with the new currency symbol.
 */
public class CrateEconomyReloadListener implements Listener {

    private final SylphianCrates plugin;

    public CrateEconomyReloadListener(SylphianCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEconomyConfigReload(EconomyConfigReloadEvent event) {
        plugin.reload(null);
    }
}
