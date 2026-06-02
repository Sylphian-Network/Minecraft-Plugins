package net.sylphian.minecraft.scoreboard.api;

import net.kyori.adventure.text.Component;

/**
 * Represents a single line of content in the player sidebar.
 *
 * @param content the Adventure Component to display on this line
 */
public record SidebarLine(Component content) {}