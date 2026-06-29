package net.sylphian.minecraft.cooking.commands;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.cooking.SylphianCooking;
import org.bukkit.command.CommandSender;

/**
 * Builds and registers the operator-only {@code /sylphian-cooking} CommandAPI command tree.
 *
 * <p>Subcommands: {@code reload}. Requires {@code sylphian.cooking.admin}.</p>
 */
public final class SylphianCookingCommand {

    private static final String PERMISSION = "sylphian.cooking.admin";

    private final SylphianCooking plugin;

    /**
     * @param plugin the owning plugin, used by the reload subcommand
     */
    public SylphianCookingCommand(SylphianCooking plugin) {
        this.plugin = plugin;
    }

    /**
     * Builds the {@code /sylphian-cooking} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("sylphian-cooking")
                .withPermission(PERMISSION)
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender))
                .then(new LiteralArgument("reload")
                        .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender)))
                .register();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /sylphian-cooking reload", NamedTextColor.RED));
    }
}
