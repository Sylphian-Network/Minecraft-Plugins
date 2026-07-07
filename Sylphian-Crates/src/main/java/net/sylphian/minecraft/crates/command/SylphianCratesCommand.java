package net.sylphian.minecraft.crates.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.crates.SylphianCrates;
import org.bukkit.command.CommandSender;

/**
 * Builds and registers the operator-only {@code /sylphian-crates} CommandAPI command tree.
 *
 * <p>Subcommands: {@code reload}. Requires {@code sylphian.crates.admin}.</p>
 */
public final class SylphianCratesCommand {

    private static final String PERMISSION = "sylphian.crates.admin";

    private final SylphianCrates plugin;

    /**
     * @param plugin the plugin instance used for reloads
     */
    public SylphianCratesCommand(SylphianCrates plugin) {
        this.plugin = plugin;
    }

    /**
     * Builds the {@code /sylphian-crates} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("sylphian-crates")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative crate commands.")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender))
                .then(new LiteralArgument("reload")
                        .executes((CommandSender sender, CommandArguments _) -> handleReload(sender)))
                .register();
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(Component.text("Reloading Sylphian Crates configuration...", NamedTextColor.YELLOW));
        plugin.reload(sender);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /sylphian-crates reload", NamedTextColor.RED));
    }
}
