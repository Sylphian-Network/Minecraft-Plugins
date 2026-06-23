package net.sylphian.minecraft.clans.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.clans.SylphianClans;
import net.sylphian.minecraft.clans.command.SubCommand;
import org.bukkit.command.CommandSender;

/** {@code /sylphian-clans reload} — re-reads config.yml and applies the new settings live. */
public final class ReloadSubCommand implements SubCommand {

    private final SylphianClans plugin;

    public ReloadSubCommand(SylphianClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("reload")
                .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender));
    }
}
