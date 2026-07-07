package net.sylphian.minecraft.items.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.items.command.SubCommand;
import net.sylphian.minecraft.items.item.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;
import java.util.Optional;

import static net.sylphian.minecraft.items.command.SylphianItemsCommand.MINI;

/** {@code /sylphian-items give <player> <item-id> [amount]} — gives a registered item to a player. */
public final class GiveSubCommand implements SubCommand {

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("give")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-items give <player> <item-id> [amount]")))
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .then(new NamespacedKeyArgument("item")
                                .replaceSuggestions(ArgumentSuggestions.strings(_ ->
                                        ItemRegistry.allNamespacedIds().toArray(new String[0])))
                                .executes((CommandSender sender, CommandArguments args) ->
                                        give(sender,
                                                (Player) Objects.requireNonNull(args.get("player")),
                                                (NamespacedKey) Objects.requireNonNull(args.get("item")),
                                                1))
                                .then(new IntegerArgument("amount", 1, 64)
                                        .executes((CommandSender sender, CommandArguments args) ->
                                                give(sender,
                                                        (Player) Objects.requireNonNull(args.get("player")),
                                                        (NamespacedKey) Objects.requireNonNull(args.get("item")),
                                                        (int) Objects.requireNonNull(args.get("amount")))))));
    }

    private void give(CommandSender sender, Player target, NamespacedKey key, int amount) {
        String itemId = key.asString();

        Optional<ItemStack> resolved = ItemRegistry.get(itemId);
        if (resolved.isEmpty()) {
            sender.sendMessage(MINI.deserialize(
                    "<red>Unknown item '" + itemId + "'. Use tab completion to see available items."));
            return;
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
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component name = meta.displayName();
            if (name != null) return MiniMessage.miniMessage().serialize(name);
        }
        return "<white>" + itemId;
    }
}
