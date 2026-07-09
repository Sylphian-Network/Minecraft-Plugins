package net.sylphian.minecraft.dimensions.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.dimensions.command.SubCommand;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import static net.sylphian.minecraft.dimensions.command.SylphianDimensionsCommand.MINI;

/**
 * {@code /sylphian-dimensions list}: every dimension with loaded/unloaded
 * status and player count.
 */
public final class ListSubCommand implements SubCommand {

    private final DimensionManager manager;

    public ListSubCommand(DimensionManager manager) {
        this.manager = manager;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("list")
                .executes((CommandSender sender, CommandArguments _) -> list(sender));
    }

    private void list(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("<yellow>--- Dimensions ---"));
        for (String name : manager.dimensionNames()) {
            World world = Bukkit.getWorld(DimensionManager.worldKey(name));
            String hubTag = name.equals(manager.hubName()) ? " <yellow>(hub)" : "";
            String status = world != null
                    ? "<green>loaded<gray>, <white>" + world.getPlayers().size() + "<gray> player(s)"
                    : "<red>not loaded";
            sender.sendMessage(MINI.deserialize("<white>" + name + hubTag + " <gray>- " + status));
        }
    }
}
