package net.sylphian.minecraft.foraging.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.foraging.SylphianForaging;
import org.bukkit.command.CommandSender;

/**
 * Operator-only {@code /sylphian-foraging} command tree: {@code reload}.
 * Requires {@code sylphian.foraging.admin}.
 */
public final class SylphianForagingCommand {

    private static final String PERMISSION = "sylphian.foraging.admin";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SylphianForaging plugin;

    public SylphianForagingCommand(SylphianForaging plugin) {
        this.plugin = plugin;
    }

    /** Builds and registers the command tree. */
    public void register() {
        new CommandTree("sylphian-foraging")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative foraging commands.")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-foraging reload")))
                .then(new LiteralArgument("reload")
                        .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender)))
                .register();
    }
}
