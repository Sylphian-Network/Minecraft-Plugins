package net.sylphian.minecraft.scoreboard.config;

import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Immutable configuration for the Sylphian Scoreboard sidebar appearance.
 *
 * @param title               MiniMessage deserialized title shown above the sidebar
 * @param showScores          whether to display numeric score values beside each line
 * @param updateIntervalTicks ticks between sidebar refreshes (20 ticks = 1 second)
 * @param headerLines         static lines rendered above all contributor sections
 * @param footerLines         static lines rendered below all contributor sections
 */
public record ScoreboardConfig(
        Component title,
        boolean showScores,
        int updateIntervalTicks,
        List<Component> headerLines,
        List<Component> footerLines
) {}