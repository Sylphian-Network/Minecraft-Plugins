package net.sylphian.minecraft.entities.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.entities.SylphianEntities;
import net.sylphian.minecraft.entities.command.admin.ReloadSubCommand;
import net.sylphian.minecraft.entities.command.admin.SpawnSubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Builds and registers the operator-only {@code /sylphian-entities} CommandAPI command tree.
 *
 * <p>Provides spawn operations for entities registered in the cross-plugin
 * {@code EntityRegistry}. Requires {@code sylphian.entities.admin}.</p>
 */
public final class SylphianEntitiesCommand {

    private static final String PERMISSION = "sylphian.entities.admin";

    public static final MiniMessage MINI = MiniMessage.miniMessage();

    private final List<SubCommand> subCommands;

    /**
     * @param plugin the plugin instance, used by the reload subcommand
     */
    public SylphianEntitiesCommand(SylphianEntities plugin) {
        this.subCommands = List.of(
                new SpawnSubCommand(),
                new ReloadSubCommand(plugin));
    }

    /**
     * Builds the {@code /sylphian-entities} tree with every admin subcommand and registers it with the CommandAPI.
     */
    public void register() {
        CommandTree tree = new CommandTree("sylphian-entities")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative entity commands.")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender));

        for (SubCommand sub : subCommands) {
            tree.then(sub.branch());
        }

        tree.register();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("""
                <yellow>--- /sylphian-entities commands ---
                <white>/sylphian-entities spawn <entity-id> [player]
                <white>/sylphian-entities reload"""));
    }
}
