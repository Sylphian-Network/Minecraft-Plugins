package net.sylphian.minecraft.crates.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.crates.gui.CratesGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the crates GUI for the executing player.
 *
 * <p>Usage: {@code /crates}</p>
 *
 * <p>Requires the {@code sylphian.crates.open} permission.</p>
 */
public class CratesCommand implements BasicCommand {

    /**
     * Opens the crates GUI for the executing player.
     *
     * @param stack the command source stack
     * @param args  the command arguments (unused)
     */
    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by a player.", NamedTextColor.RED));
            return;
        }

        CratesGUI.open(player);
    }

    /**
     * Restricts this command to senders with the open permission.
     *
     * @param sender the command sender
     * @return true if the sender has {@code sylphian.crates.open}
     */
    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("sylphian.crates.open");
    }
}