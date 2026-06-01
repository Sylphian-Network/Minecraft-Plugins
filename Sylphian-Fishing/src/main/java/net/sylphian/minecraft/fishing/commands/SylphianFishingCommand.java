package net.sylphian.minecraft.fishing.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.fishing.SylphianFishing;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Root command for Sylphian Fishing administrative actions.
 *
 * <p>Usage: {@code /sylphian-fishing <subcommand>}</p>
 *
 * <ul>
 *   <li>{@code reload} — reloads config.yml and fish.yml without restarting</li>
 * </ul>
 */
public class SylphianFishingCommand implements BasicCommand {

    private final SylphianFishing plugin;

    /**
     * Constructs a new SylphianFishingCommand.
     *
     * @param plugin the plugin instance used to trigger reloads
     */
    public SylphianFishingCommand(SylphianFishing plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the command, routing to the appropriate subcommand handler.
     *
     * @param stack the command source stack
     * @param args  the command arguments
     */
    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(Component.text("Usage: /sylphian-fishing reload", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("Reloading Sylphian Fishing configuration...", NamedTextColor.YELLOW));
        plugin.reload(sender);
    }

    /**
     * Provides tab completion for subcommands.
     *
     * @param stack the command source stack
     * @param args  the current arguments
     * @return available subcommand suggestions
     */
    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }

    /**
     * Restricts this command to senders with the admin permission.
     *
     * @param sender the command sender
     * @return true if the sender has {@code sylphian.fishing.admin}
     */
    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("sylphian.fishing.admin");
    }
}