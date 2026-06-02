package net.sylphian.minecraft.profile.utils;

import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.profile.SylphianProfile;
import net.sylphian.minecraft.profile.UserProfile;
import net.sylphian.minecraft.scoreboard.services.NametagService;
import org.bukkit.entity.Player;

/**
 * Manages the visual representation of player profiles in-game.
 * Handles the formatting of display names (incorporating forum names),
 * tab list appearance, nametags via scoreboards, and custom chat rendering.
 */
public class VisualManager {

    private final SylphianProfile plugin;

    /**
     * Constructs a new VisualManager.
     * @param plugin the plugin instance
     */
    public VisualManager(SylphianProfile plugin) {
        this.plugin = plugin;
    }

    /**
     * Formats a player's full display name, including their forum username as a prefix if linked.
     * The forum prefix is clickable and redirects to their forum profile.
     *
     * @param player  the player instance
     * @param profile the player's UserProfile
     * @return a Component representing the formatted name
     */
    public Component formatFullDisplayName(Player player, UserProfile profile) {
        String forumName = profile.forumUsername();
        // Fallback to plain name if not linked to a forum account
        if (forumName == null) {
            return Component.text(player.getName(), NamedTextColor.WHITE);
        }

        String forumBase = plugin.getConfig().getString("forum_base_url", "https://example.com/community");
        String profileUrl = forumBase + "/members/" + forumName + "." + profile.xfUserId();

        // Build name: [ForumName] PlayerName
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(forumName, NamedTextColor.WHITE))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .hoverEvent(Component.text("View " + forumName + "'s forum profile"))
                .clickEvent(ClickEvent.openUrl(profileUrl));
    }

    /**
     * Updates all visual elements for a player.
     *
     * @param player  the player to update
     * @param profile the player's UserProfile
     */
    public void updateVisuals(Player player, UserProfile profile) {
        Component fullDisplayName = formatFullDisplayName(player, profile);
        // Update tab list name
        player.playerListName(fullDisplayName);
        // Update nametag above head
        applyNametag(player, profile);
    }

    /**
     * Applies a forum prefix to the player's nametag using Minecraft scoreboard teams.
     *
     * @param player  the player instance
     * @param profile the player's UserProfile
     */
    public void applyNametag(Player player, UserProfile profile) {
        String forumName = profile.forumUsername();
        if (forumName == null) return;

        String forumBase = plugin.getConfig().getString("forum_base_url", "https://example.com/community");
        String profileUrl = forumBase + "/members/" + forumName + "." + profile.xfUserId();

        Component prefix = Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(forumName, NamedTextColor.WHITE)
                        .hoverEvent(Component.text("View " + forumName + "'s forum profile"))
                        .clickEvent(ClickEvent.openUrl(profileUrl)))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));

        NametagService.setNametag("f_" + profile.xfUserId(), player.getName(), prefix);
    }

    /**
     * Gets a chat renderer that incorporates the player's full display name.
     *
     * @param player  the player instance
     * @param profile the player's UserProfile
     * @return a ChatRenderer instance
     */
    public ChatRenderer getChatRenderer(Player player, UserProfile profile) {
        Component fullDisplayName = formatFullDisplayName(player, profile);
        Component separator = Component.text(" » ", NamedTextColor.DARK_GRAY);

        return (source, sourceDisplayName, message, viewer) ->
                Component.empty()
                        .append(fullDisplayName)
                        .append(separator)
                        .append(message.color(NamedTextColor.WHITE));
    }
}
