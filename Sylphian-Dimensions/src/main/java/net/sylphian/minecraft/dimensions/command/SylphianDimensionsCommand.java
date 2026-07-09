package net.sylphian.minecraft.dimensions.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.dimensions.SylphianDimensions;
import net.sylphian.minecraft.dimensions.command.admin.InfoSubCommand;
import net.sylphian.minecraft.dimensions.command.admin.ListSubCommand;
import net.sylphian.minecraft.dimensions.command.admin.MigrateSubCommand;
import net.sylphian.minecraft.dimensions.command.admin.ReloadSubCommand;
import net.sylphian.minecraft.dimensions.command.admin.SendSubCommand;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import net.sylphian.minecraft.dimensions.world.TemplateManager;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Builds and registers the operator-only {@code /sylphian-dimensions} CommandAPI command tree.
 *
 * <p>Requires {@code sylphian.dimensions.admin}.</p>
 */
public final class SylphianDimensionsCommand {

    private static final String PERMISSION = "sylphian.dimensions.admin";

    public static final MiniMessage MINI = MiniMessage.miniMessage();

    private final List<SubCommand> subCommands;

    /**
     * @param plugin    the owning plugin, used by the reload and migrate subcommands
     * @param manager   the dimension manager, used by the migrate subcommand
     * @param templates the template manager, used by the migrate subcommand
     */
    public SylphianDimensionsCommand(SylphianDimensions plugin, DimensionManager manager, TemplateManager templates) {
        this.subCommands = List.of(
                new ReloadSubCommand(plugin),
                new MigrateSubCommand(plugin, manager, templates),
                new InfoSubCommand(manager, templates),
                new ListSubCommand(manager),
                new SendSubCommand(manager));
    }

    /**
     * Builds the {@code /sylphian-dimensions} tree with every admin subcommand and registers it with the CommandAPI.
     */
    public void register() {
        CommandTree tree = new CommandTree("sylphian-dimensions")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative dimension commands.")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender));

        for (SubCommand sub : subCommands) {
            tree.then(sub.branch());
        }

        tree.register();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("""
                <yellow>--- /sylphian-dimensions commands ---
                <white>/sylphian-dimensions reload
                /sylphian-dimensions migrate <dimension>
                /sylphian-dimensions info <dimension>
                /sylphian-dimensions list
                /sylphian-dimensions send <player> <dimension>"""));
    }
}
