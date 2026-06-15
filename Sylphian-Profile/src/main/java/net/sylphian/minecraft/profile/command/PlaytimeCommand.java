package net.sylphian.minecraft.profile.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.profile.service.PlayerService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Command to view a player's total cumulative playtime.
 * Uses the PlayerService to calculate real-time playtime including the current session.
 */
public class PlaytimeCommand implements BasicCommand {
    private final PlayerService playerService;
    private final Plugin plugin;

    /**
     * Constructs a new PlaytimeCommand.
     *
     * @param playerService the player service for playtime lookups
     * @param plugin        the plugin logger for error reporting
     */
    public PlaytimeCommand(PlayerService playerService, Plugin plugin) {
        this.playerService = playerService;
        this.plugin = plugin;
    }

    /**
     * Executes the playtime command.
     * Fetches playtime asynchronously and sends a formatted message to the player.
     *
     * @param stack the command source stack
     * @param args  command arguments (currently unused)
     */
    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        // Fetch playtime from database + current session
        playerService.getTotalPlaytime(player.getUniqueId())
                .thenAccept(totalPlaytime ->
                        player.getServer().getScheduler().runTask(plugin, () -> sendPlaytime(player, player.getName(), totalPlaytime)))
                .exceptionally(ex -> {
                    player.sendMessage(Component.text("Failed to retrieve playtime.", NamedTextColor.RED));
                    plugin.getLogger().severe("Failed to retrieve playtime: " + ex.getMessage());
                    return null;
                });
    }

    /**
     * The permission required to view playtime. Declared in paper-plugin.yml
     * with {@code default: true}, so all players have it.
     *
     * @return the permission node for this command
     */
    @Override
    public String permission() {
        return "sylphian.profile.playtime";
    }

    /**
     * Formats and sends the playtime message to a player.
     *
     * @param player       the recipient
     * @param username     the username to display in the message
     * @param totalSeconds the total playtime in seconds
     */
    private void sendPlaytime(Player player, String username, long totalSeconds) {
        // Break down seconds into days, hours, minutes, and seconds
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