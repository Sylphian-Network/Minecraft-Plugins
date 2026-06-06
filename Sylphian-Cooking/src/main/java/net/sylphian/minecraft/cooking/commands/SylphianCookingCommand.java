package net.sylphian.minecraft.cooking.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.cooking.SylphianCooking;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Root administrative command for Sylphian-Cooking.
 *
 * <p>Usage: {@code /sylphian-cooking <subcommand>}</p>
 *
 * <ul>
 *   <li>{@code reload} — reloads {@code config.yml} and {@code recipes.yml} without restarting</li>
 * </ul>
 *
 * <p>Requires the {@code sylphian.cooking.admin} permission.</p>
 */
public class SylphianCookingCommand implements BasicCommand {
    private final SylphianCooking plugin;

    /**
     * Constructs a new SylphianCookingCommand.
     *
     * @param plugin the plugin instance used for reloads
     */
    public SylphianCookingCommand(SylphianCooking plugin) { this.plugin = plugin; }

    /**
     * Routes execution to the appropriate subcommand handler.
     *
     * @param stack the command source stack
     * @param args  the command arguments
     */
    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (args.length == 0) { sendUsage(sender); return; }
        switch (args[0].toLowerCase()) {
            case "reload" -> plugin.reload(sender);
            default       -> sendUsage(sender);
        }
    }

    /**
     * Provides tab completion for all subcommands.
     *
     * @param stack the command source stack
     * @param args  the current arguments
     * @return available suggestions for the current argument position
     */
    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) return List.of("reload");
        return List.of();
    }

    /**
     * Restricts this command to senders with the admin permission.
     *
     * @param sender the command sender
     * @return true if the sender has {@code sylphian.cooking.admin}
     */
    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("sylphian.cooking.admin");
    }

    /**
     * Sends usage information listing all available subcommands.
     *
     * @param sender the command sender to notify
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /sylphian-cooking reload", NamedTextColor.RED));
    }
}
