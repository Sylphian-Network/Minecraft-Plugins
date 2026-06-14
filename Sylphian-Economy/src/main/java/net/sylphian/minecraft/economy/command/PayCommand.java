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

/** {@code /pay <player> <amount>} — transfers money to another online player. */
public class PayCommand implements BasicCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final EconomyAPI economy;

    public PayCommand(EconomyAPI economy) {
        this.economy = economy;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, @NonNull String[] args) {
        CommandSender sender = source.getSender();

        if (!(sender instanceof Player payer)) {
            sender.sendMessage(MINI.deserialize("<red>Only players can send money."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(MINI.deserialize("<red>Usage: /pay <player> <amount>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(MINI.deserialize("<red>Player '" + args[0] + "' is not online."));
            return;
        }
        if (target.getUniqueId().equals(payer.getUniqueId())) {
            sender.sendMessage(MINI.deserialize("<red>You cannot pay yourself."));
            return;
        }

        BigDecimal amount = MoneyFormat.parse(args[1]);
        if (amount == null || amount.signum() <= 0) {
            sender.sendMessage(MINI.deserialize("<red>'" + args[1] + "' is not a valid positive amount."));
            return;
        }

        economy.transfer(payer.getUniqueId(), target.getUniqueId(), amount).thenAccept(success -> {
            if (success) {
                String formatted = MoneyFormat.format(amount);
                payer.sendMessage(MINI.deserialize(
                        "<green>You paid <gold>" + formatted + "</gold> to " + target.getName() + ".</green>"));
                target.sendMessage(MINI.deserialize(
                        "<green>You received <gold>" + formatted + "</gold> from " + payer.getName() + ".</green>"));
            } else {
                payer.sendMessage(MINI.deserialize("<red>You don't have enough money for that."));
            }
        });
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
        return "sylphian.economy.pay";
    }
}
