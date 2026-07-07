package net.sylphian.minecraft.economy.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.economy.api.EconomyAPI;
import net.sylphian.minecraft.economy.util.MoneyFormat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Builds and registers {@code /balance [player]} (aliases {@code bal}, {@code money}).
 * Shows your own balance or another online player's. Resolves online players only.
 */
public final class BalanceCommand {

    private static final String PERMISSION = "sylphian.economy.balance";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final EconomyAPI economy;

    public BalanceCommand(EconomyAPI economy) {
        this.economy = economy;
    }

    /**
     * Builds the {@code /balance} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("balance")
                .withPermission(PERMISSION)
                .withShortDescription("View a player's balance.")
                .withAliases("bal", "money")
                .executesPlayer((Player player, CommandArguments _) ->
                        economy.getBalance(player.getUniqueId()).thenAccept(balance ->
                                player.sendMessage(MINI.deserialize("<green>Your balance: <gold>" + MoneyFormat.format(balance) + "</gold></green>"))))
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .executes((CommandSender sender, CommandArguments args) -> {
                            Player target = (Player) args.get("player");
                            economy.getBalance(target.getUniqueId()).thenAccept(balance ->
                                    sender.sendMessage(MINI.deserialize("<green>" + target.getName() + "'s balance: <gold>" + MoneyFormat.format(balance) + "</gold></green>")));
                        }))
                .register();
    }
}
