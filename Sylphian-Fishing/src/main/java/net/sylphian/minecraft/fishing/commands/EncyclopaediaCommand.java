package net.sylphian.minecraft.fishing.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.sylphian.minecraft.fishing.gui.EncyclopaediaMenu;
import org.bukkit.entity.Player;

/**
 * Command to open the fish encyclopaedia GUI for a player.
 * Usage: /encyclopaedia
 */
public class EncyclopaediaCommand implements BasicCommand {

    private final EncyclopaediaMenu menu;

    /**
     * Constructs a new EncyclopaediaCommand.
     *
     * @param menu the encyclopaedia menu to open
     */
    public EncyclopaediaCommand(EncyclopaediaMenu menu) {
        this.menu = menu;
    }

    /**
     * Executes the /encyclopaedia command.
     * Opens the first page of the encyclopaedia for the player.
     *
     * @param stack the command source stack
     * @param args  command arguments (none expected)
     */
    @Override
    public void execute(CommandSourceStack stack, String[] args) {

        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendPlainMessage("Players only.");
            return;
        }

        menu.open(player, 0);
    }

    /**
     * The permission required to open the encyclopaedia. Declared in
     * paper-plugin.yml with {@code default: true}, so all players have it.
     *
     * @return the permission node for this command
     */
    @Override
    public String permission() {
        return "sylphian.fishing.encyclopaedia";
    }
}