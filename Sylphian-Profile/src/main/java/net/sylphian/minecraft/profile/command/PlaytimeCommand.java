package net.sylphian.minecraft.profile.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.profile.service.PlayerService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Builds and registers the player-facing {@code /playtime} CommandAPI command.
 * Shows a player's total cumulative playtime, calculated in real time including
 * the current session.
 */
public final class PlaytimeCommand {

    private static final String PERMISSION = "sylphian.profile.playtime";

    private final PlayerService playerService;
    private final Plugin plugin;

    /**
     * @param playerService the player service for playtime lookups
     * @param plugin        the plugin logger for error reporting
     */
    public PlaytimeCommand(PlayerService playerService, Plugin plugin) {
        this.playerService = playerService;
        this.plugin = plugin;
    }

    /**
     * Builds the {@code /playtime} tree and registers it with the CommandAPI.
     */
    public void register() {
        new CommandTree("playtime")
                .withPermission(PERMISSION)
                .withShortDescription("View your playtime on the server.")
                .executesPlayer((Player player, CommandArguments _) -> execute(player))
                .register();
    }

    private void execute(Player player) {
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
     * Formats and sends the playtime message to a player.
     *
     * @param player       the recipient
     * @param username     the username to display in the message
     * @param totalSeconds the total playtime in seconds
     */
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
