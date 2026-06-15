package net.sylphian.minecraft.economy;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.economy.api.EconomyProvider;
import net.sylphian.minecraft.economy.command.BalanceCommand;
import net.sylphian.minecraft.economy.command.EconomyAdminCommand;
import net.sylphian.minecraft.economy.command.PayCommand;
import net.sylphian.minecraft.economy.db.migrations.Migration001CreateBalances;
import net.sylphian.minecraft.economy.db.repositories.EconomyRepository;
import net.sylphian.minecraft.economy.event.EconomyConfigReloadEvent;
import net.sylphian.minecraft.economy.event.PlayerBalanceChangeEvent;
import net.sylphian.minecraft.economy.listener.EconomyListener;
import net.sylphian.minecraft.economy.service.BalanceChangePublisher;
import net.sylphian.minecraft.economy.service.EconomyService;
import net.sylphian.minecraft.economy.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.List;

/**
 * Main plugin class for Sylphian-Economy.
 */
public final class SylphianEconomy extends JavaPlugin {

    private EconomyService economyService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        MoneyFormat.configure(getConfig().getString("currency-symbol", "$"));

        DatabaseService.registerMigrations(List.of(new Migration001CreateBalances()));
        DatabaseService.runMigrations("Sylphian-Economy", getLogger());

        BigDecimal startingBalance = BigDecimal.valueOf(getConfig().getDouble("starting-balance", 0.0));

        EconomyRepository repository = new EconomyRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor());

        BalanceChangePublisher publisher = playerId ->
                getServer().getScheduler().runTask(this, () ->
                        getServer().getPluginManager().callEvent(new PlayerBalanceChangeEvent(playerId)));

        this.economyService = new EconomyService(repository, startingBalance, publisher);

        EconomyProvider.register(economyService);

        getServer().getPluginManager().registerEvents(new EconomyListener(economyService, this), this);
        Bukkit.getOnlinePlayers().forEach(player -> economyService.load(player.getUniqueId()));

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("balance", "View your or another player's balance.",
                    List.of("bal", "money"), new BalanceCommand(economyService));
            commands.register("pay", "Send money to another online player.",
                    new PayCommand(economyService, this));
            commands.register("economy", "Administrative economy commands.",
                    new EconomyAdminCommand(economyService, this::reload));
        });

        getLogger().info("Sylphian-Economy initialized.");
    }

    /**
     * Reloads config-driven settings (currency symbol, starting balance) from disk
     * and notifies consumers via {@link EconomyConfigReloadEvent}.
     */
    public void reload() {
        reloadConfig();
        MoneyFormat.configure(getConfig().getString("currency-symbol", "$"));
        economyService.setStartingBalance(BigDecimal.valueOf(getConfig().getDouble("starting-balance", 0.0)));
        getServer().getPluginManager().callEvent(new EconomyConfigReloadEvent());
    }

    @Override
    public void onDisable() {
        EconomyProvider.unregister();
        getLogger().info("Sylphian-Economy disabled.");
    }
}
