package net.sylphian.minecraft.scoreboard.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Represents a single line of content in the player sidebar.
 *
 * @param content the Adventure Component to display on this line
 */
public record SidebarLine(Component content) {
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Constructs a line from a MiniMessage string. */
    public static SidebarLine of(String miniMessage) {
        return new SidebarLine(MINI.deserialize(miniMessage));
    }

    /** Constructs a line from an already-built Component. */
    public static SidebarLine of(Component component) {
        return new SidebarLine(component);
    }

    /** Returns a blank separator line. */
    public static SidebarLine blank() {
        return new SidebarLine(Component.empty());
    }
}