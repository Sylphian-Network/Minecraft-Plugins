package net.sylphian.minecraft.dimensions.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.dimensions.SylphianDimensions;
import net.sylphian.minecraft.dimensions.command.SubCommand;
import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import net.sylphian.minecraft.dimensions.world.TemplateManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.Objects;

import static net.sylphian.minecraft.dimensions.command.SylphianDimensionsCommand.MINI;

/**
 * {@code /sylphian-dimensions migrate <dimension>}: evacuates players, unloads
 * the world without saving, re-copies it from its template, and reloads it
 * live. No restart required.
 */
public final class MigrateSubCommand implements SubCommand {

    private final SylphianDimensions plugin;
    private final DimensionManager manager;
    private final TemplateManager templates;

    public MigrateSubCommand(SylphianDimensions plugin, DimensionManager manager, TemplateManager templates) {
        this.plugin = plugin;
        this.manager = manager;
        this.templates = templates;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("migrate")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-dimensions migrate <dimension>")))
                .then(new StringArgument("dimension")
                        .replaceSuggestions(ArgumentSuggestions.strings(_ ->
                                manager.dimensionNames().toArray(new String[0])))
                        .executes((CommandSender sender, CommandArguments args) ->
                                migrate(sender, (String) Objects.requireNonNull(args.get("dimension")))));
    }

    private void migrate(CommandSender sender, String name) {
        if (!plugin.reload(sender)) return;

        Dimension dimension = manager.getDimension(name).orElse(null);
        if (dimension == null) {
            sender.sendMessage(MINI.deserialize("<red>Unknown dimension '" + name + "'."));
            return;
        }

        if (!manager.unloadForMigration(name)) {
            sender.sendMessage(MINI.deserialize("<red>Could not unload the world for '" + name + "'; is it loaded?"));
            return;
        }

        sender.sendMessage(MINI.deserialize("<gray>Migrating <white>" + name + "<gray> from template '" + dimension.template() + "'..."));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                templates.recopy(dimension);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (manager.loadDimension(dimension)) {
                        sender.sendMessage(MINI.deserialize("<green>Dimension '" + name + "' migrated and reloaded."));
                    } else {
                        sender.sendMessage(MINI.deserialize("<red>Re-copy succeeded but the world failed to load; check the console."));
                    }
                });
            } catch (IOException e) {
                plugin.getLogger().severe("Migration of '" + name + "' failed: " + e);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(MINI.deserialize("<red>Migration failed: " + e.getMessage() + ". The world is unloaded; fix the template and migrate again.")));
            }
        });
    }
}
