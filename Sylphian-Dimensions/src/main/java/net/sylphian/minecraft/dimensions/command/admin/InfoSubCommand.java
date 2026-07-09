package net.sylphian.minecraft.dimensions.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.dimensions.command.SubCommand;
import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import net.sylphian.minecraft.dimensions.world.TemplateManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.Objects;

import static net.sylphian.minecraft.dimensions.command.SylphianDimensionsCommand.MINI;

/**
 * {@code /sylphian-dimensions info <dimension>}: shows the live resolved
 * ruleset, template details, copy timestamp, world status, and player count.
 */
public final class InfoSubCommand implements SubCommand {

    private final DimensionManager manager;
    private final TemplateManager templates;

    public InfoSubCommand(DimensionManager manager, TemplateManager templates) {
        this.manager = manager;
        this.templates = templates;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("info")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-dimensions info <dimension>")))
                .then(new StringArgument("dimension")
                        .replaceSuggestions(ArgumentSuggestions.strings(_ ->
                                manager.dimensionNames().toArray(new String[0])))
                        .executes((CommandSender sender, CommandArguments args) ->
                                info(sender, (String) Objects.requireNonNull(args.get("dimension")))));
    }

    private void info(CommandSender sender, String name) {
        Dimension dimension = manager.getDimension(name).orElse(null);
        if (dimension == null) {
            sender.sendMessage(MINI.deserialize("<red>Unknown dimension '" + name + "'."));
            return;
        }

        World world = Bukkit.getWorld(DimensionManager.worldKey(name));
        String status = world != null
                ? "<green>loaded<gray>, <white>" + world.getPlayers().size() + "<gray> player(s)"
                : "<red>not loaded";
        String hubTag = name.equals(manager.hubName()) ? " <yellow>(hub)" : "";

        sender.sendMessage(MINI.deserialize(
                "<yellow>--- Dimension '" + name + "'" + hubTag + " <yellow>---\n"
                + "<gray>World: " + status + "\n"
                + dimension.describe() + "\n"
                + "<gray>Copied: <white>" + templates.copiedAt(dimension) + "\n"
                + "<gray>Rules: " + dimension.ruleset().describe()));
    }
}
