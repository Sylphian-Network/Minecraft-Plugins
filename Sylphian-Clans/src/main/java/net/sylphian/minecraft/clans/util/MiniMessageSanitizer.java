package net.sylphian.minecraft.clans.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** Strips click/hover/insertion from player-supplied MiniMessage before it is stored or echoed. */
public final class MiniMessageSanitizer {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MiniMessageSanitizer() {}

    /** Parses raw MiniMessage and re-serialises it with all interactivity removed. */
    public static String sanitize(String raw) {
        return MINI.serialize(strip(MINI.deserialize(raw)));
    }

    private static Component strip(Component c) {
        Component out = c.clickEvent(null).hoverEvent(null).insertion(null);
        return out.children(out.children().stream().map(MiniMessageSanitizer::strip).toList());
    }
}