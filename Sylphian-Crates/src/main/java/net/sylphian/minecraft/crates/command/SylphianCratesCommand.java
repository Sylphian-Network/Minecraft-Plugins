package net.sylphian.minecraft.crates.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.crates.SylphianCrates;
import net.sylphian.minecraft.crates.config.KeyConfig;
import net.sylphian.minecraft.crates.key.CrateKey;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Root administrative command for Sylphian-Crates.
 *
 * <p>Usage: {@code /sylphian-crates <subcommand>}</p>
 *
 * <ul>
 *   <li>{@code reload} — reloads keys.yml and crates.yml without restarting</li>
 *   <li>{@code give <player> <key-id> [amount]} — gives a player one or more crate keys</li>
 * </ul>
 *
 * <p>Requires the {@code sylphian.crates.admin} permission.</p>
 */
public class SylphianCratesCommand implements BasicCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SylphianCrates plugin;

    /**
     * Constructs a new SylphianCratesCommand.
     *
     * @param plugin the plugin instance used for reloads and key access
     */
    public SylphianCratesCommand(SylphianCrates plugin) {
        this.plugin = plugin;
    }

    /**
     * Routes execution to the appropriate subcommand handler.
     *
     * @param stack the command source stack
     * @param args  the command arguments
     */
    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "give"   -> handleGive(sender, args);
            default       -> sendUsage(sender);
        }
    }

    /**
     * Reloads keys.yml and crates.yml from disk.
     *
     * @param sender the command sender to notify
     */
    private void handleReload(CommandSender sender) {
        sender.sendMessage(Component.text("Reloading Sylphian Crates configuration...", NamedTextColor.YELLOW));
        plugin.reload(sender);
    }

    /**
     * Gives a player one or more crate keys by ID.
     * Usage: {@code /sylphian-crates give <player> <key-id> [amount]}
     *
     * @param sender the command sender
     * @param args   the full args array
     */
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /sylphian-crates give <player> <key-id> [amount]", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[1] + "' is not online.", NamedTextColor.RED));
            return;
        }

        KeyConfig keyConfig = plugin.getKeys().get(args[2]);
        if (keyConfig == null) {
            sender.sendMessage(Component.text("Unknown key '" + args[2] + "'. Valid keys: "
                    + plugin.getKeys().keySet(), NamedTextColor.RED));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage(Component.text("Amount must be between 1 and 64.", NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid amount: '" + args[3] + "'.", NamedTextColor.RED));
                return;
            }
        }

        ItemStack key = CrateKey.create(keyConfig, plugin);
        key.setAmount(amount);
        target.getInventory().addItem(key).values()
                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));

        sender.sendMessage(MINI.deserialize(
                "<gray>Gave <white>" + amount + "x " + keyConfig.displayName()
                        + " <gray>to <white>" + target.getName() + "<gray>."));
        target.sendMessage(MINI.deserialize(
                "<gray>You received <white>" + amount + "x " + keyConfig.displayName() + "<gray>."));
    }

    /**
     * Sends usage information listing all available subcommands.
     *
     * @param sender the command sender to notify
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        sender.sendMessage(Component.text("  /sylphian-crates reload", NamedTextColor.RED));
        sender.sendMessage(Component.text("  /sylphian-crates give <player> <key-id> [amount]", NamedTextColor.RED));
    }

    /**
     * Provides tab completion for all subcommands and their arguments.
     *
     * @param stack the command source stack
     * @param args  the current arguments
     * @return available suggestions for the current argument position
     */
    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            return List.of("reload", "give");
        }

        return switch (args[0].toLowerCase()) {
            case "give" -> switch (args.length) {
                case 2 -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
                case 3 -> plugin.getKeys().keySet().stream().toList();
                case 4 -> List.of("1", "4", "8", "16", "32", "64");
                default -> List.of();
            };
            default -> List.of();
        };
    }

    /**
     * Restricts this command to senders with the admin permission.
     *
     * @param sender the command sender
     * @return true if the sender has {@code sylphian.crates.admin}
     */
    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("sylphian.crates.admin");
    }
}