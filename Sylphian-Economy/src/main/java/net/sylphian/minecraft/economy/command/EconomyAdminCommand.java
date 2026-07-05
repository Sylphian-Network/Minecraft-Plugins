package net.sylphian.minecraft.economy.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.economy.api.EconomyAPI;
import net.sylphian.minecraft.economy.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@code /economy <give|take|set> <player> <amount>} - administrative balance management.
 * {@code /economy reload} reloads the config. Targets online players only.
 */
public class EconomyAdminCommand implements BasicCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final EconomyAPI economy;
    private final Runnable reloadAction;

    public EconomyAdminCommand(EconomyAPI economy, Runnable reloadAction) {
        this.economy = economy;
        this.reloadAction = reloadAction;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, @NonNull String[] args) {
        CommandSender sender = source.getSender();

        if (args.length == 0) {
            sender.sendMessage(MINI.deserialize("<red>Usage: /economy <give|take|set|reload> ..."));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadAction.run();
            sender.sendMessage(MINI.deserialize("<green>Economy configuration reloaded.</green>"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(MINI.deserialize("<red>Usage: /economy <give|take|set> <player> <amount>"));
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(MINI.deserialize("<red>Player '" + args[1] + "' is not online."));
            return;
        }

        BigDecimal amount = MoneyFormat.parse(args[2]);
        boolean requiresPositive = action.equals("give") || action.equals("take");
        if (amount == null || (requiresPositive ? amount.signum() <= 0 : amount.signum() < 0)) {
            sender.sendMessage(MINI.deserialize("<red>'" + args[2] + "' is not a valid amount."));
            return;
        }

        switch (action) {
            case "give" -> economy.deposit(target.getUniqueId(), amount).thenRun(() ->
                    sender.sendMessage(MINI.deserialize(
                            "<green>Gave <gold>" + MoneyFormat.format(amount) + "</gold> to " + target.getName() + ".</green>")));
            case "take" -> economy.withdraw(target.getUniqueId(), amount).thenAccept(success ->
                    sender.sendMessage(success
                            ? MINI.deserialize("<green>Took <gold>" + MoneyFormat.format(amount) + "</gold> from " + target.getName() + ".</green>")
                            : MINI.deserialize("<red>" + target.getName() + " does not have that much to take.")));
            case "set" -> economy.set(target.getUniqueId(), amount).thenRun(() ->
                    sender.sendMessage(MINI.deserialize(
                            "<green>Set " + target.getName() + "'s balance to <gold>" + MoneyFormat.format(amount) + "</gold>.</green>")));
            default -> sender.sendMessage(MINI.deserialize("<red>Unknown action '" + action + "'. Use give, take, or set."));
        }
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, @NonNull String[] args) {
        if (args.length <= 1) {
            String prefix = (args.length == 0 ? "" : args[0]).toLowerCase();
            return Stream.of("give", "take", "set", "reload").filter(action -> action.startsWith(prefix)).toList();
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    @Override
    public @NonNull String permission() {
        return "sylphian.economy.admin";
    }
}
