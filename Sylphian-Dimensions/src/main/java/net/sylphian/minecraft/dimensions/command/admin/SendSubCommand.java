package net.sylphian.minecraft.dimensions.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.dimensions.command.SubCommand;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

import static net.sylphian.minecraft.dimensions.command.SylphianDimensionsCommand.MINI;

/**
 * {@code /sylphian-dimensions send <player> <dimension>}: teleports another
 * player into a dimension. Console-usable; uses the same entry path as
 * {@code /dimension}.
 */
public final class SendSubCommand implements SubCommand {

    private final DimensionManager manager;

    public SendSubCommand(DimensionManager manager) {
        this.manager = manager;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("send")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-dimensions send <player> <dimension>")))
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .then(new StringArgument("dimension")
                                .replaceSuggestions(ArgumentSuggestions.strings(_ ->
                                        manager.enterableNames().toArray(new String[0])))
                                .executes((CommandSender sender, CommandArguments args) ->
                                        send(sender, (Player) Objects.requireNonNull(args.get("player")), (String) Objects.requireNonNull(args.get("dimension"))))));
    }

    private void send(CommandSender sender, Player target, String name) {
        if (!manager.enter(target, name)) {
            sender.sendMessage(MINI.deserialize("<red>Unknown dimension '" + name + "' or its world is not loaded."));
            return;
        }
        sender.sendMessage(MINI.deserialize("<gray>Sent <white>" + target.getName() + " <gray>to <white>" + name + "<gray>."));
        target.sendMessage(MINI.deserialize("<gray>You were sent to <white>" + name + "<gray>."));
    }
}
