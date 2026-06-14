package net.sylphian.minecraft.economy.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired on the main thread after the economy config is reloaded.
 *
 * <p>Consumers should refresh any state derived from the economy config, mainly
 * the currency symbol (e.g. re-render cached displays, reparse configs).</p>
 */
public class EconomyConfigReloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
