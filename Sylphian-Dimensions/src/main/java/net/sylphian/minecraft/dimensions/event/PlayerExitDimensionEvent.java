package net.sylphian.minecraft.dimensions.event;

import net.sylphian.minecraft.dimensions.model.Dimension;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a player leaves a managed dimension,
 * whether by command, teleport, or disconnect. Not cancellable.
 */
public class PlayerExitDimensionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final Dimension dimension;

    /**
     * @param playerUuid the player who left the dimension
     * @param dimension  the dimension left
     */
    public PlayerExitDimensionEvent(UUID playerUuid, Dimension dimension) {
        this.playerUuid = playerUuid;
        this.dimension = dimension;
    }

    /** @return the player who left the dimension */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return the dimension left */
    public Dimension getDimension() { return dimension; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
