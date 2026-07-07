package net.sylphian.minecraft.crates.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.crates.gui.CratesGUI;
import org.bukkit.entity.Player;

/**
 * Builds and registers the player-facing {@code /crates} CommandAPI command.
 * Opens the crates GUI for the executing player.
 */
public final class CratesCommand {

    private static final String PERMISSION = "sylphian.crates.open";

    /**
     * Builds the {@code /crates} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("crates")
                .withPermission(PERMISSION)
                .withShortDescription("Open crates.")
                .executesPlayer((Player player, CommandArguments _) -> CratesGUI.open(player))
                .register();
    }
}
