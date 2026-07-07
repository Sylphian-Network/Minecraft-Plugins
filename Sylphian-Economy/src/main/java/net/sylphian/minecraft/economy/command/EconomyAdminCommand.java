package net.sylphian.minecraft.economy.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.economy.api.EconomyAPI;
import net.sylphian.minecraft.economy.util.MoneyFormat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

/**
 * Builds and registers the operator-only {@code /economy} CommandAPI command tree.
 *
 * <p>Subcommands: {@code give|take|set <player> <amount>}, {@code reload}.
 * Requires {@code sylphian.economy.admin}. Targets online players only.</p>
 */
public final class EconomyAdminCommand {

    private static final String PERMISSION = "sylphian.economy.admin";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final EconomyAPI economy;
    private final Runnable reloadAction;

    public EconomyAdminCommand(EconomyAPI economy, Runnable reloadAction) {
        this.economy = economy;
        this.reloadAction = reloadAction;
    }

    /**
     * Builds the {@code /economy} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("economy")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative economy commands.")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender))
                .then(new LiteralArgument("reload")
                        .executes((CommandSender sender, CommandArguments _) -> {
                            reloadAction.run();
                            sender.sendMessage(MINI.deserialize("<green>Economy configuration reloaded.</green>"));
                        }))
                .then(amountBranch("give", true))
                .then(amountBranch("take", true))
                .then(amountBranch("set", false))
                .register();
    }

    private Argument<String> amountBranch(String action, boolean requiresPositive) {
        return new LiteralArgument(action)
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .then(new StringArgument("amount")
                                .executes((CommandSender sender, CommandArguments args) ->
                                        handleAction(sender, action, requiresPositive,
                                                (Player) args.get("player"), (String) args.get("amount")))));
    }

    private void handleAction(CommandSender sender, String action, boolean requiresPositive, Player target, String rawAmount) {
        BigDecimal amount = MoneyFormat.parse(rawAmount);
        if (amount == null || (requiresPositive ? amount.signum() <= 0 : amount.signum() < 0)) {
            sender.sendMessage(MINI.deserialize("<red>'" + rawAmount + "' is not a valid amount."));
            return;
        }

        switch (action) {
            case "give" -> economy.deposit(target.getUniqueId(), amount).thenRun(() ->
                    sender.sendMessage(MINI.deserialize("<green>Gave <gold>" + MoneyFormat.format(amount) + "</gold> to " + target.getName() + ".</green>")));
            case "take" -> economy.withdraw(target.getUniqueId(), amount).thenAccept(success ->
                    sender.sendMessage(success
                            ? MINI.deserialize("<green>Took <gold>" + MoneyFormat.format(amount) + "</gold> from " + target.getName() + ".</green>")
                            : MINI.deserialize("<red>" + target.getName() + " does not have that much to take.")));
            case "set" -> economy.set(target.getUniqueId(), amount).thenRun(() ->
                    sender.sendMessage(MINI.deserialize("<green>Set " + target.getName() + "'s balance to <gold>" + MoneyFormat.format(amount) + "</gold>.</green>")));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("<red>Usage: /economy <give|take|set|reload> ..."));
    }
}
