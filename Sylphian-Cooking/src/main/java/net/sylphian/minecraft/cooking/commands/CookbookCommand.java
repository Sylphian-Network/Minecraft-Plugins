package net.sylphian.minecraft.cooking.commands;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.cooking.gui.RecipeBookMenu;
import org.bukkit.entity.Player;

/**
 * Builds and registers the player-facing {@code /cookbook} CommandAPI command.
 * Opens the recipe book GUI for the executing player.
 */
public final class CookbookCommand {

    private static final String PERMISSION = "sylphian.cooking.book";

    private final RecipeBookMenu menu;

    /**
     * @param menu the recipe book menu to open
     */
    public CookbookCommand(RecipeBookMenu menu) {
        this.menu = menu;
    }

    /**
     * Builds the {@code /cookbook} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("cookbook")
                .withPermission(PERMISSION)
                .withShortDescription("Open the recipe book.")
                .executesPlayer((Player player, CommandArguments _) -> menu.open(player, 0))
                .register();
    }
}
