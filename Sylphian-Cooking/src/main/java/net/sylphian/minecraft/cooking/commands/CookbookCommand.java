package net.sylphian.minecraft.cooking.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.sylphian.minecraft.cooking.gui.RecipeBookMenu;
import org.bukkit.entity.Player;

/**
 * Opens the recipe book GUI for the executing player.
 * Usage: /cookbook
 */
public final class CookbookCommand implements BasicCommand {

    private final RecipeBookMenu menu;

    /**
     * @param menu the recipe book menu to open
     */
    public CookbookCommand(RecipeBookMenu menu) {
        this.menu = menu;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendPlainMessage("Players only.");
            return;
        }
        menu.open(player, 0);
    }

    @Override
    public String permission() {
        return "sylphian.cooking.book";
    }
}
