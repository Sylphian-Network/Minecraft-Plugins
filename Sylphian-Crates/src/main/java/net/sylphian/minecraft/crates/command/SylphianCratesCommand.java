package net.sylphian.minecraft.crates.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.crates.SylphianCrates;
import org.bukkit.command.CommandSender;
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
 * </ul>
 *
 * <p>Requires the {@code sylphian.crates.admin} permission.</p>
 */
public class SylphianCratesCommand implements BasicCommand {

    private final SylphianCrates plugin;

    /**
     * Constructs a new SylphianCratesCommand.
     *
     * @param plugin the plugin instance used for reloads
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
     * Sends usage information listing all available subcommands.
     *
     * @param sender the command sender to notify
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        sender.sendMessage(Component.text("  /sylphian-crates reload", NamedTextColor.RED));
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
        if (args.length <= 1) return List.of("reload");
        return List.of();
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