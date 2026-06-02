package net.sylphian.minecraft.scoreboard.services;

import net.kyori.adventure.text.Component;
import net.sylphian.minecraft.scoreboard.ScoreboardService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sub-service responsible for nametag team management.
 *
 * <p>Applies prefix teams to all online player scoreboards so that
 * nametag prefixes are visible server-wide. Delegates scoreboard access to
 * {@link ScoreboardService#getScoreboard(java.util.UUID)}.</p>
 *
 * <p>External plugins should call {@link #setNametag} and {@link #clearNametagEntry}.
 * {@link #applyToScoreboard} and {@link #clear} are called internally by
 * {@link ScoreboardService}.</p>
 */
public class NametagService {

    private static final Map<String, NametagTeam> nametags = new LinkedHashMap<>();

    private NametagService() {}

    /**
     * Registers or updates a nametag team across all online player scoreboards.
     *
     * @param teamId the unique team identifier, e.g. {@code "f_12345"}
     * @param entry  the score entry — typically the player's name
     * @param prefix the prefix Component shown before the player's name
     */
    public static void setNametag(String teamId, String entry, Component prefix) {
        NametagTeam team = nametags.computeIfAbsent(teamId, id -> new NametagTeam(prefix));
        team.setPrefix(prefix);
        team.entries().add(entry);

        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = ScoreboardService.getScoreboard(online.getUniqueId());
            if (scoreboard == null) continue;
            applyTeam(scoreboard, teamId, team);
        }
    }

    /**
     * Removes an entry from whichever nametag team it belongs to across all
     * online player scoreboards. Unregisters the team if it becomes empty.
     *
     * @param entry the entry to remove — typically the player's name
     */
    public static void clearNametagEntry(String entry) {
        nametags.entrySet().removeIf(e -> {
            e.getValue().entries().remove(entry);
            return e.getValue().entries().isEmpty();
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = ScoreboardService.getScoreboard(online.getUniqueId());
            if (scoreboard == null) continue;
            Team team = scoreboard.getEntryTeam(entry);
            if (team == null) continue;
            team.removeEntry(entry);
            if (team.getEntries().isEmpty()) team.unregister();
        }
    }

    /**
     * Applies all registered nametag teams to a newly created scoreboard.
     * Called by {@link ScoreboardService#onJoin} when a player receives their scoreboard.
     *
     * @param scoreboard the player's newly created scoreboard
     */
    public static void applyToScoreboard(Scoreboard scoreboard) {
        for (Map.Entry<String, NametagTeam> entry : nametags.entrySet()) {
            applyTeam(scoreboard, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Clears all nametag state. Called by {@link ScoreboardService#shutdown()}.
     */
    public static void clear() {
        nametags.clear();
    }

    private static void applyTeam(Scoreboard scoreboard, String teamId, NametagTeam data) {
        Team team = scoreboard.getTeam(teamId);
        if (team == null) team = scoreboard.registerNewTeam(teamId);
        team.prefix(data.prefix());
        for (String entry : data.entries()) {
            if (!team.hasEntry(entry)) team.addEntry(entry);
        }
    }
}