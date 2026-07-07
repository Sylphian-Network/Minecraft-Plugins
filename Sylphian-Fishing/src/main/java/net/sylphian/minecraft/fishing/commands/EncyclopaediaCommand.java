package net.sylphian.minecraft.fishing.commands;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.fishing.gui.EncyclopaediaMenu;
import org.bukkit.entity.Player;

/**
 * Builds and registers the player-facing {@code /encyclopaedia} CommandAPI command.
 * Opens the first page of the fish encyclopaedia for the executing player.
 */
public final class EncyclopaediaCommand {

    private static final String PERMISSION = "sylphian.fishing.encyclopaedia";

    private final EncyclopaediaMenu menu;

    /**
     * @param menu the encyclopaedia menu to open
     */
    public EncyclopaediaCommand(EncyclopaediaMenu menu) {
        this.menu = menu;
    }

    /**
     * Builds the {@code /encyclopaedia} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("encyclopaedia")
                .withPermission(PERMISSION)
                .withShortDescription("Open the fish encyclopaedia.")
                .executesPlayer((Player player, CommandArguments _) -> menu.open(player, 0))
                .register();
    }
}
