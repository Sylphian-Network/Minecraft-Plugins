package net.sylphian.minecraft.logging.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.logging.SylphianLogging;
import org.bukkit.command.CommandSender;

/**
 * Operator-only {@code /sylphian-logging} command tree: {@code reload}.
 * Requires {@code sylphian.logging.admin}.
 */
public final class SylphianLoggingCommand {

    private static final String PERMISSION = "sylphian.logging.admin";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SylphianLogging plugin;

    public SylphianLoggingCommand(SylphianLogging plugin) {
        this.plugin = plugin;
    }

    /** Builds and registers the command tree. */
    public void register() {
        new CommandTree("sylphian-logging")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative logging commands.")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-logging reload")))
                .then(new LiteralArgument("reload")
                        .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender)))
                .register();
    }
}
