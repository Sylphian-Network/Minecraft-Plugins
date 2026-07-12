package net.sylphian.minecraft.dimensions.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.dimensions.command.SylphianDimensionsCommand.MINI;

/**
 * Player-facing {@code /hub} command: returns the player to the hub from anywhere.
 *
 * <p>Requires {@code sylphian.dimensions.use}.</p>
 */
public final class HubCommand {

    private static final String PERMISSION = "sylphian.dimensions.use";

    private final DimensionManager manager;

    public HubCommand(DimensionManager manager) {
        this.manager = manager;
    }

    /**
     * Builds the {@code /hub} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("hub")
                .withPermission(PERMISSION)
                .withShortDescription("Return to the hub.")
                .executesPlayer((Player player, CommandArguments _) -> {
                    if (manager.toHub(player)) {
                        player.sendMessage(MINI.deserialize("<gray>Returned to the hub."));
                    } else {
                        player.sendMessage(MINI.deserialize("<red>The hub is not available right now."));
                    }
                })
                .register();
    }
}
