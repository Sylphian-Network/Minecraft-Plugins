package net.sylphian.minecraft.entities.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.entities.SylphianEntities;
import net.sylphian.minecraft.entities.command.SubCommand;
import org.bukkit.command.CommandSender;

/** {@code /sylphian-entities reload}: rebuilds the entity definitions from disk. */
public final class ReloadSubCommand implements SubCommand {

    private final SylphianEntities plugin;

    public ReloadSubCommand(SylphianEntities plugin) {
        this.plugin = plugin;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("reload")
                .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender));
    }
}
