package net.sylphian.minecraft.fishing.commands.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.fishing.SylphianFishing;
import net.sylphian.minecraft.fishing.commands.SubCommand;
import org.bukkit.command.CommandSender;

/** {@code /sylphian-fishing reload} — re-reads all config files and applies the new settings live. */
public final class ReloadSubCommand implements SubCommand {

    private final SylphianFishing plugin;

    public ReloadSubCommand(SylphianFishing plugin) {
        this.plugin = plugin;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("reload")
                .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender));
    }
}
