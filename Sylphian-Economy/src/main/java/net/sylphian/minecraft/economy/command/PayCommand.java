package net.sylphian.minecraft.economy.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.economy.api.EconomyAPI;
import net.sylphian.minecraft.economy.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.Objects;

/** Builds and registers {@code /pay <player> <amount>}, transferring money to another online player. */
public final class PayCommand {

    private static final String PERMISSION = "sylphian.economy.pay";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final EconomyAPI economy;
    private final JavaPlugin plugin;

    public PayCommand(EconomyAPI economy, JavaPlugin plugin) {
        this.economy = economy;
        this.plugin = plugin;
    }

    /**
     * Builds the {@code /pay} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("pay")
                .withPermission(PERMISSION)
                .withShortDescription("Send money to another online player.")
                .executesPlayer((Player player, CommandArguments _) ->
                        player.sendMessage(MINI.deserialize("<red>Usage: /pay <player> <amount>")))
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .executesPlayer((Player player, CommandArguments _) ->
                                player.sendMessage(MINI.deserialize("<red>Usage: /pay <player> <amount>")))
                        .then(new StringArgument("amount")
                                .executesPlayer((Player payer, CommandArguments args) ->
                                        handlePay(payer, (Player) Objects.requireNonNull(args.get("player")), (String) args.get("amount")))))
                .register();
    }

    private void handlePay(Player payer, Player target, String rawAmount) {
        if (target.getUniqueId().equals(payer.getUniqueId())) {
            payer.sendMessage(MINI.deserialize("<red>You cannot pay yourself."));
            return;
        }

        BigDecimal amount = MoneyFormat.parse(rawAmount);
        if (amount == null || amount.signum() <= 0) {
            payer.sendMessage(MINI.deserialize("<red>'" + rawAmount + "' is not a valid positive amount."));
            return;
        }

        economy.transfer(payer.getUniqueId(), target.getUniqueId(), amount).thenAccept(success ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        String formatted = MoneyFormat.format(amount);
                        payer.sendMessage(MINI.deserialize("<green>You paid <gold>" + formatted + "</gold> to " + target.getName() + ".</green>"));
                        target.sendMessage(MINI.deserialize("<green>You received <gold>" + formatted + "</gold> from " + payer.getName() + ".</green>"));
                    } else {
                        payer.sendMessage(MINI.deserialize("<red>You don't have enough money for that."));
                    }
                }));
    }
}
