package net.sylphian.minecraft.dimensions.event;

import net.sylphian.minecraft.dimensions.model.Dimension;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a player enters a managed dimension,
 * whether by command, teleport, or login. Not cancellable.
 */
public class PlayerEnterDimensionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final Dimension dimension;

    /**
     * @param playerUuid the player who entered the dimension
     * @param dimension  the dimension entered
     */
    public PlayerEnterDimensionEvent(UUID playerUuid, Dimension dimension) {
        this.playerUuid = playerUuid;
        this.dimension = dimension;
    }

    /** @return the player who entered the dimension */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return the dimension entered */
    public Dimension getDimension() { return dimension; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
