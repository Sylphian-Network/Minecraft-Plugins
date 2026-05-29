package net.sylphian.minecraft.fishing.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.sylphian.minecraft.fishing.gui.EncyclopaediaMenu;
import org.bukkit.entity.Player;

public class EncyclopaediaCommand implements BasicCommand {

    private final EncyclopaediaMenu menu;

    public EncyclopaediaCommand(EncyclopaediaMenu menu) {
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
}