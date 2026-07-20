package net.sylphian.minecraft.dimensions.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.entity.Player;

import java.util.Objects;

import static net.sylphian.minecraft.dimensions.command.SylphianDimensionsCommand.MINI;

/**
 * Player-facing {@code /dimension [name]} command: teleports into a dimension,
 * or lists available dimensions when no name is given.
 *
 * <p>Requires {@code sylphian.dimensions.use}.</p>
 */
public final class DimensionCommand {

    private static final String PERMISSION = "sylphian.dimensions.use";

    private final DimensionManager manager;

    public DimensionCommand(DimensionManager manager) {
        this.manager = manager;
    }

    /**
     * Builds the {@code /dimension} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("dimension")
                .withPermission(PERMISSION)
                .withShortDescription("Travel to a dimension.")
                .executesPlayer((Player player, CommandArguments _) -> list(player))
                .then(new StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings(_ ->
                                manager.enterableNames().toArray(new String[0])))
                        .executesPlayer((Player player, CommandArguments args) -> {
                            String name = (String) Objects.requireNonNull(args.get("name"));
                            if (manager.enter(player, name)) {
                                player.sendMessage(MINI.deserialize("<gray>Entered <white>" + name + "<gray>."));
                            } else {
                                player.sendMessage(MINI.deserialize("<red>Unknown dimension '" + name + "'."));
                            }
                        }))
                .register();
    }

    private void list(Player player) {
        player.sendMessage(MINI.deserialize("<yellow>Available dimensions: <white>" + String.join("<gray>, <white>", manager.enterableNames())));
    }
}
