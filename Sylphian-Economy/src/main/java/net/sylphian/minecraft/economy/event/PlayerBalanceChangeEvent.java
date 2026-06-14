package net.sylphian.minecraft.economy.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a player's balance changes.
 *
 * <p>An invalidation signal: it carries only the affected player's UUID, not the
 * new amount. Listeners that need the value should read it via
 * {@link net.sylphian.minecraft.economy.api.EconomyProvider}.</p>
 */
public class PlayerBalanceChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;

    /**
     * @param playerId the UUID of the player whose balance changed
     */
    public PlayerBalanceChangeEvent(UUID playerId) {
        this.playerId = playerId;
    }

    /**
     * @return the UUID of the player whose balance changed
     */
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
