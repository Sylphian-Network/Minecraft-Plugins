package net.sylphian.minecraft.profile.utils;

import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.profile.SylphianProfile;
import net.sylphian.minecraft.profile.UserProfile;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class VisualManager {

    private final SylphianProfile plugin;

    public VisualManager(SylphianProfile plugin) {
        this.plugin = plugin;
    }

    public Component formatFullDisplayName(Player player, UserProfile profile) {
        String forumName = profile.forumUsername();
        if (forumName == null) {
            return Component.text(player.getName(), NamedTextColor.WHITE);
        }

        String forumBase = plugin.getConfig().getString("forum_base_url", "https://example.com/community");
        String profileUrl = forumBase + "/members/" + forumName + "." + profile.xfUserId();

        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(forumName, NamedTextColor.WHITE))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .hoverEvent(Component.text("View " + forumName + "'s forum profile"))
                .clickEvent(ClickEvent.openUrl(profileUrl));
    }

    public void updateVisuals(Player player, UserProfile profile) {
        Component fullDisplayName = formatFullDisplayName(player, profile);
        player.playerListName(fullDisplayName);
        applyNametag(player, profile);
    }

    public void applyNametag(Player player, UserProfile profile) {
        String forumName = profile.forumUsername();
        if (forumName == null) return;

        Scoreboard scoreboard = plugin.getScoreboard();
        if (scoreboard == null) return;

        String teamName = "f_" + profile.xfUserId();
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        team.prefix(
                Component.text("[", NamedTextColor.DARK_GRAY)
                        .append(Component.text(forumName, NamedTextColor.WHITE))
                        .append(Component.text("] ", NamedTextColor.DARK_GRAY))
        );

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public ChatRenderer getChatRenderer(Player player, UserProfile profile) {
        Component fullDisplayName = formatFullDisplayName(player, profile);
        Component separator = Component.text(" » ", NamedTextColor.DARK_GRAY);

        return (source, sourceDisplayName, message, viewer) ->
                Component.empty()
                        .append(fullDisplayName)
                        .append(separator)
                        .append(message.color(NamedTextColor.WHITE));
    }

    public void cleanUpPlayer(Player player) {
        Scoreboard scoreboard = plugin.getScoreboard();
        if (scoreboard == null) return;

        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }
}
