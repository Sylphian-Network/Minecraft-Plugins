package net.sylphian.minecraft.skills.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.skills.SylphianSkills;
import net.sylphian.minecraft.skills.command.admin.ReloadSubCommand;
import net.sylphian.minecraft.skills.command.admin.WatchSubCommand;
import net.sylphian.minecraft.skills.command.admin.XpSubCommand;
import net.sylphian.minecraft.skills.service.SkillsService;
import org.bukkit.command.CommandSender;

import java.util.List;

import static net.sylphian.minecraft.skills.command.admin.SkillsAdminContext.MINI;

/**
 * Builds and registers the operator-only {@code /sylphian-skills} CommandAPI command tree.
 * Requires {@code sylphian.skills.admin}.
 */
public final class SkillsAdminCommand {

    private static final String PERMISSION = "sylphian.skills.admin";

    private final List<SubCommand> subCommands;

    /**
     * @param plugin  the owning plugin, passed through to subcommands that need it
     * @param service the skills service, used by subcommands that read or write player data
     */
    public SkillsAdminCommand(SylphianSkills plugin, SkillsService service) {
        this.subCommands = List.of(
                new ReloadSubCommand(plugin),
                new XpSubCommand(service),
                new WatchSubCommand(service));
    }

    /**
     * Builds the {@code /sylphian-skills} tree with every admin subcommand and registers
     * it with the CommandAPI.
     */
    public void register() {
        CommandTree tree = new CommandTree("sylphian-skills")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative skills commands.")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender));

        for (SubCommand sub : subCommands) {
            tree.then(sub.branch());
        }

        tree.register();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("""
                <yellow>--- /sylphian-skills commands ---
                <white>/sylphian-skills reload
                /sylphian-skills xp add <player> <skill> <amount>
                /sylphian-skills xp set <player> <skill> <amount>
                /sylphian-skills xp remove <player> <skill> <amount>
                /sylphian-skills watch <skill> <player>"""));
    }
}
