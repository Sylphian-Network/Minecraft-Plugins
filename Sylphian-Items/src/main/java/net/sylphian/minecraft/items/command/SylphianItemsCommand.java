package net.sylphian.minecraft.items.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.items.command.admin.GiveSubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Builds and registers the operator-only {@code /sylphian-items} CommandAPI command tree.
 *
 * <p>Provides give operations for items registered in the cross-plugin
 * {@code ItemRegistry}. Requires {@code sylphian.items.admin}.</p>
 */
public final class SylphianItemsCommand {

    private static final String PERMISSION = "sylphian.items.admin";

    public static final MiniMessage MINI = MiniMessage.miniMessage();

    private final List<SubCommand> subCommands = List.of(
            new GiveSubCommand());

    /**
     * Builds the {@code /sylphian-items} tree with every admin subcommand and registers it with the CommandAPI.
     */
    public void register() {
        CommandTree tree = new CommandTree("sylphian-items")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative item commands.")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender));

        for (SubCommand sub : subCommands) {
            tree.then(sub.branch());
        }

        tree.register();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("""
                <yellow>--- /sylphian-items commands ---
                <white>/sylphian-items give <player> <item-id> [amount]"""));
    }
}
