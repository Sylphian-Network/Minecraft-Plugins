package net.sylphian.minecraft.core.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.core.item.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Root administrative command for Sylphian Core.
 *
 * <p>Usage: {@code /sylphian-core <subcommand>}</p>
 *
 * <ul>
 *   <li>{@code give <player> <item-id> [amount]} — gives a player any item registered
 *       in the cross-plugin {@link ItemRegistry}, referenced by its namespaced ID
 *       (e.g. {@code sylphian-crates:legendary_key} or
 *       {@code sylphian-fishing:bait/ocean_bait})</li>
 * </ul>
 *
 * <p>Requires the {@code sylphian.core.admin} permission.</p>
 */
public class SylphianCoreCommand implements BasicCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

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
            case "give" -> handleGive(sender, args);
            default     -> sendUsage(sender);
        }
    }

    /**
     * Gives a player one or more items resolved from the cross-plugin item registry.
     * Usage: {@code /sylphian-core give <player> <item-id> [amount]}
     *
     * @param sender the command sender
     * @param args   the full args array
     */
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text(
                    "Usage: /sylphian-core give <player> <item-id> [amount]", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[1] + "' is not online.", NamedTextColor.RED));
            return;
        }

        String itemId = args[2];
        Optional<ItemStack> resolved = ItemRegistry.get(itemId);
        if (resolved.isEmpty()) {
            sender.sendMessage(Component.text(
                    "Unknown item '" + itemId + "'. Use tab completion to see available items.",
                    NamedTextColor.RED));
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

        ItemStack item = resolved.get().clone();
        item.setAmount(amount);

        target.getInventory().addItem(item).values()
                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));

        String displayName = getDisplayName(item, itemId);
        sender.sendMessage(MINI.deserialize(
                "<gray>Gave <white>" + amount + "x " + displayName + " <gray>to <white>" + target.getName() + "<gray>."));
        target.sendMessage(MINI.deserialize(
                "<gray>You received <white>" + amount + "x " + displayName + "<gray>."));
    }

    /**
     * Returns the display name of an item as a MiniMessage string.
     * Falls back to the namespaced item ID if no display name is set on the item.
     *
     * @param item   the item to read the name from
     * @param itemId the namespaced item ID used as a fallback
     * @return the display name string
     */
    private String getDisplayName(ItemStack item, String itemId) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return MiniMessage.miniMessage().serialize(item.getItemMeta().displayName());
        }
        return "<white>" + itemId;
    }

    /**
     * Sends usage information listing all available subcommands.
     *
     * @param sender the command sender to notify
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        sender.sendMessage(Component.text("  /sylphian-core give <player> <item-id> [amount]", NamedTextColor.RED));
    }

    /**
     * Provides tab completion for all subcommands and their arguments.
     * Item IDs are sourced live from the {@link ItemRegistry}.
     *
     * @param stack the command source stack
     * @param args  the current arguments
     * @return available suggestions for the current argument position
     */
    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) return List.of("give");

        return switch (args[0].toLowerCase()) {
            case "give" -> switch (args.length) {
                case 2 -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
                case 3 -> ItemRegistry.allNamespacedIds().stream()
                        .filter(id -> id.startsWith(args[2]))
                        .toList();
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
     * @return true if the sender has {@code sylphian.core.admin}
     */
    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("sylphian.core.admin");
    }
}
