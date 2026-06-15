package net.sylphian.minecraft.scoreboard.services;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.sylphian.minecraft.scoreboard.ScoreboardService;
import net.sylphian.minecraft.scoreboard.api.SidebarContributor;
import net.sylphian.minecraft.scoreboard.api.SidebarLine;
import net.sylphian.minecraft.scoreboard.config.ScoreboardConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Sub-service responsible for sidebar content.
 *
 * <p>Manages contributor registration and renders sidebar lines to each player's
 * managed scoreboard. Scoreboard access is delegated to
 * {@link ScoreboardService#getScoreboard(java.util.UUID)}.</p>
 *
 * <p>External plugins register contributors via {@link #registerContributor} and
 * trigger immediate updates via {@link #refresh}. {@link #setup}, {@link #update},
 * and {@link #clear} are called internally by {@link ScoreboardService}.</p>
 */
public class SidebarService {

    private static final int MAX_LINES = 15;
    private static final Logger LOGGER = Logger.getLogger(SidebarService.class.getName());

    private static final TreeMap<Integer, SidebarContributor> contributors = new TreeMap<>();
    private static final Map<String, SidebarContributor> contributorsById = new LinkedHashMap<>();

    private static ScoreboardConfig config = new ScoreboardConfig(
            Component.text("Sylphian Network"), false, 10,
            Collections.emptyList(), Collections.emptyList());

    private SidebarService() {}

    /**
     * Applies the loaded configuration to the sidebar service.
     * Called internally by {@link ScoreboardService#init(JavaPlugin, ScoreboardConfig)}.
     *
     * @param scoreboardConfig the parsed scoreboard configuration
     */
    public static void configure(ScoreboardConfig scoreboardConfig) {
        config = scoreboardConfig;
    }

    /**
     * Registers a sidebar contributor. Lower priority values appear higher.
     *
     * @param contributor the contributor to register
     * @throws IllegalArgumentException if a contributor with the same ID or priority is already registered
     */
    public static void registerContributor(SidebarContributor contributor) {
        if (contributorsById.containsKey(contributor.getId())) {
            LOGGER.warning("Sidebar contributor '" + contributor.getId()
                    + "' is already registered, skipping duplicate registration.");
            return;
        }
        if (contributors.containsKey(contributor.getPriority())) {
            LOGGER.warning("Sidebar contributor '" + contributor.getId()
                    + "' has a duplicate priority " + contributor.getPriority()
                    + " (conflicts with '" + contributors.get(contributor.getPriority()).getId()
                    + "'). [Skipping]");
            return;
        }
        contributorsById.put(contributor.getId(), contributor);
        contributors.put(contributor.getPriority(), contributor);
    }

    /**
     * Removes a contributor by ID and refreshes all online players.
     *
     * @param id the contributor ID to remove
     */
    public static void unregisterContributor(String id) {
        SidebarContributor removed = contributorsById.remove(id);
        if (removed != null) {
            contributors.remove(removed.getPriority());
            ScoreboardService.refreshAll();
        }
    }

    /**
     * Immediately refreshes the sidebar for a single player.
     *
     * @param player the player to refresh
     */
    public static void refresh(Player player) {
        ScoreboardService.refresh(player);
    }

    /**
     * Sets up the sidebar objective and pre-allocated line slots on a new scoreboard.
     * Called by {@link ScoreboardService#onJoin}.
     *
     * @param scoreboard the player's newly created scoreboard
     */
    public static void setup(Scoreboard scoreboard) {
        Objective objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, config.title());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        if (!config.showScores()) {
            objective.numberFormat(NumberFormat.blank());
        }
    }

    /**
     * Renders the current sidebar content for the given player onto their scoreboard.
     * Called by {@link ScoreboardService#refresh} and {@link ScoreboardService#refreshAll}.
     *
     * @param scoreboard the player's managed scoreboard
     * @param player     the player to render lines for
     */
    public static void update(Scoreboard scoreboard, Player player) {
        Objective objective = scoreboard.getObjective("sidebar");
        if (objective == null) return;

        List<Component> lines = buildLines(player);
        if (lines.size() > MAX_LINES) lines = lines.subList(0, MAX_LINES);

        for (int i = 0; i < MAX_LINES; i++) {
            String entry = fakeEntry(i);
            if (i < lines.size()) {
                Score score = objective.getScore(entry);
                score.customName(lines.get(i));
                score.setScore(lines.size() - i);
            } else {
                scoreboard.resetScores(entry);
            }
        }
    }

    /**
     * Clears all contributor state. Called by {@link ScoreboardService#shutdown()}.
     */
    public static void clear() {
        contributors.clear();
        contributorsById.clear();
    }

    private static List<Component> buildLines(Player player) {
        List<Component> contributorLines = new ArrayList<>();
        Set<String> exhaustedGroups = new HashSet<>();
        boolean firstSection = true;

        for (SidebarContributor contributor : contributors.values()) {
            String group = contributor.getExclusionGroup();
            if (group != null && exhaustedGroups.contains(group)) continue;

            List<SidebarLine> section = contributor.getLinesFor(player);
            if (section.isEmpty()) continue;

            if (group != null) exhaustedGroups.add(group);
            if (!firstSection) contributorLines.add(Component.empty());
            section.forEach(line -> contributorLines.add(line.content()));
            firstSection = false;
        }

        List<Component> lines = new ArrayList<>(config.headerLines());

        if (!contributorLines.isEmpty()) {
            if (!config.headerLines().isEmpty()) lines.add(Component.empty());
            lines.addAll(contributorLines);
        }

        if (!config.footerLines().isEmpty()) {
            if (!contributorLines.isEmpty() || !config.headerLines().isEmpty()) {
                lines.add(Component.empty());
            }
            lines.addAll(config.footerLines());
        }

        return lines;
    }

    private static String fakeEntry(int slot) {
        return "__sb_" + slot + "__";
    }
}