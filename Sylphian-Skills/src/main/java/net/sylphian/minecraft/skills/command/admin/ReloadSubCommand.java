package net.sylphian.minecraft.skills.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.skills.SylphianSkills;
import net.sylphian.minecraft.skills.command.SubCommand;
import org.bukkit.command.CommandSender;

/** {@code /sylphian-skills reload}: re-reads config.yml and applies the new settings live. */
public final class ReloadSubCommand implements SubCommand {

    private final SylphianSkills plugin;

    public ReloadSubCommand(SylphianSkills plugin) {
        this.plugin = plugin;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("reload")
                .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender));
    }
}
