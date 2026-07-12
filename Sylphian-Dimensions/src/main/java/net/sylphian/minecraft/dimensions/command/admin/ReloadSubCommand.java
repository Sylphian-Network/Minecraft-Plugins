package net.sylphian.minecraft.dimensions.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.dimensions.SylphianDimensions;
import net.sylphian.minecraft.dimensions.command.SubCommand;
import org.bukkit.command.CommandSender;

/** {@code /sylphian-dimensions reload}: rebuilds the configuration from disk. */
public final class ReloadSubCommand implements SubCommand {

    private final SylphianDimensions plugin;

    public ReloadSubCommand(SylphianDimensions plugin) {
        this.plugin = plugin;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("reload")
                .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender));
    }
}
