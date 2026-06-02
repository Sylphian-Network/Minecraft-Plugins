package net.sylphian.minecraft.scoreboard.api;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Interface for plugins that wish to contribute content to the player sidebar.
 *
 * <p>Contributors are registered once at plugin startup via
 * {@link net.sylphian.minecraft.scoreboard.services.SidebarService#registerContributor(SidebarContributor)}.
 * On each refresh, {@link #getLinesFor(Player)} is called to retrieve
 * the current lines for a given player. Returning an empty list hides
 * the section entirely — no blank space is left.</p>
 *
 * <p>Contributors are sorted by {@link #getPriority()} — lower values
 * appear higher in the sidebar. If multiple contributors return content,
 * a blank separator line is inserted between sections automatically.</p>
 */
public interface SidebarContributor {

    /**
     * Returns the unique identifier for this contributor.
     * Used for registration and removal.
     *
     * @return the contributor ID, e.g. {@code "sylphian-fishing-bait"}
     */
    String getId();

    /**
     * Returns the priority that controls this contributor's vertical
     * position relative to others. Lower values appear higher.
     *
     * @return the priority value
     */
    int getPriority();

    /**
     * Returns the sidebar lines to display for the given player.
     * This is called on every refresh — keep it fast.
     * Return an empty list to hide this contributor's section entirely.
     *
     * @param player the player to get lines for
     * @return ordered list of lines, top to bottom, or empty to hide
     */
    List<SidebarLine> getLinesFor(Player player);
}