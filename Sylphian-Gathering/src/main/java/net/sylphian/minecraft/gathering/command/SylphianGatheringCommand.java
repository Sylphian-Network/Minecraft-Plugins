package net.sylphian.minecraft.gathering.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.SylphianGathering;
import net.sylphian.minecraft.gathering.world.LiveNode;
import net.sylphian.minecraft.gathering.world.NodeManager;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.TreeMap;

/**
 * Operator-only {@code /sylphian-gathering} command tree: {@code reload} and a
 * {@code list} debug view. Requires {@code sylphian.gathering.admin}.
 */
public final class SylphianGatheringCommand {

    private static final String PERMISSION = "sylphian.gathering.admin";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SylphianGathering plugin;
    private final NodeManager nodeManager;

    public SylphianGatheringCommand(SylphianGathering plugin, NodeManager nodeManager) {
        this.plugin = plugin;
        this.nodeManager = nodeManager;
    }

    /** Builds and registers the command tree. */
    public void register() {
        new CommandTree("sylphian-gathering")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative gathering commands.")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender))
                .then(new LiteralArgument("reload")
                        .executes((CommandSender sender, CommandArguments _) -> plugin.reload(sender)))
                .then(new LiteralArgument("list")
                        .executes((CommandSender sender, CommandArguments _) -> sendList(sender)))
                .register();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-gathering <reload|list>"));
    }

    private void sendList(CommandSender sender) {
        Map<String, int[]> byType = new TreeMap<>();
        int total = 0;
        for (LiveNode node : nodeManager.liveNodes()) {
            int[] counts = byType.computeIfAbsent(node.type().id(), _ -> new int[2]);
            if (node.state() == LiveNode.State.AVAILABLE) counts[0]++;
            else counts[1]++;
            total++;
        }

        sender.sendMessage(MINI.deserialize("<gold>Gathering nodes <gray>(<white>" + total + "</white> live):"));
        if (byType.isEmpty()) {
            sender.sendMessage(MINI.deserialize("<gray>  (none)"));
            return;
        }
        for (Map.Entry<String, int[]> entry : byType.entrySet()) {
            sender.sendMessage(MINI.deserialize("<gray>  <white>" + entry.getKey()
                    + "</white>: <green>" + entry.getValue()[0] + "</green> available, <red>"
                    + entry.getValue()[1] + "</red> depleted"));
        }
    }
}
