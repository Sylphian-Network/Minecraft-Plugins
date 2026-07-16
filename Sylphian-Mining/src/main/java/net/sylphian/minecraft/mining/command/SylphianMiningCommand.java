package net.sylphian.minecraft.mining.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.mining.SylphianMining;
import org.bukkit.command.CommandSender;

/**
 * Operator-only {@code /sylphian-mining} command tree: {@code reload}.
 * Requires {@code sylphian.mining.admin}.
 */
public final class SylphianMiningCommand {

    private static final String PERMISSION = "sylphian.mining.admin";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SylphianMining plugin;

    public SylphianMiningCommand(SylphianMining plugin) {
        this.plugin = plugin;
    }

    /** Builds and registers the command tree. */
    public void register() {
        new CommandTree("sylphian-mining")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative mining commands.")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-mining reload")))
                .then(new LiteralArgument("reload")
                        .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender)))
                .register();
    }
}
