package net.sylphian.minecraft.profile.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.profile.service.PlayerService;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class PlaytimeCommand implements BasicCommand {
    private final PlayerService playerService;
    private final Logger logger;

    public PlaytimeCommand(PlayerService playerService, Logger logger) {
        this.playerService = playerService;
        this.logger = logger;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        playerService.getTotalPlaytime(player.getUniqueId()).thenAccept(totalPlaytime -> sendPlaytime(player, player.getName(), totalPlaytime)).exceptionally(ex -> {
            player.sendMessage(Component.text("Failed to retrieve playtime.", NamedTextColor.RED));
            logger.severe("Failed to retrieve playtime: " + ex.getMessage());
            return null;
        });
    }

    private void sendPlaytime(Player player, String username, long totalSeconds) {
        long days    = totalSeconds / 86400;
        long hours   = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        player.sendMessage(
                Component.text("Playtime for ", NamedTextColor.GRAY)
                        .append(Component.text(username, NamedTextColor.GOLD))
                        .append(Component.text(":", NamedTextColor.GRAY))
        );
        player.sendMessage(
                Component.text("  " + days + "d ", NamedTextColor.YELLOW)
                        .append(Component.text(hours + "h ", NamedTextColor.YELLOW))
                        .append(Component.text(minutes + "m ", NamedTextColor.YELLOW))
                        .append(Component.text(seconds + "s", NamedTextColor.YELLOW))
        );
    }
}