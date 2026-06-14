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

import java.util.Collection;
import java.util.List;

/**
 * {@code /balance [player]} - shows your own balance or another online player's.
 * Resolves online players only.
 */
public class BalanceCommand implements BasicCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final EconomyAPI economy;

    public BalanceCommand(EconomyAPI economy) {
        this.economy = economy;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, @NonNull String[] args) {
        CommandSender sender = source.getSender();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MINI.deserialize("<red>Only players can check their own balance. Use /balance <player>."));
                return;
            }
            economy.getBalance(player.getUniqueId()).thenAccept(balance ->
                    player.sendMessage(MINI.deserialize(
                            "<green>Your balance: <gold>" + MoneyFormat.format(balance) + "</gold></green>")));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(MINI.deserialize("<red>Player '" + args[0] + "' is not online."));
            return;
        }
        economy.getBalance(target.getUniqueId()).thenAccept(balance ->
                sender.sendMessage(MINI.deserialize(
                        "<green>" + target.getName() + "'s balance: <gold>"
                                + MoneyFormat.format(balance) + "</gold></green>")));
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, @NonNull String[] args) {
        if (args.length <= 1) {
            String prefix = (args.length == 0 ? "" : args[0]).toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    @Override
    public @NonNull String permission() {
        return "sylphian.economy.balance";
    }
}
