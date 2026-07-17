package net.sylphian.minecraft.dimensions.event;

import net.sylphian.minecraft.dimensions.model.Dimension;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired on the main thread after a dimension is re-copied from its template and
 * reloaded live, replacing its Bukkit world instance. Not cancellable.
 */
public class DimensionMigratedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Dimension dimension;

    /**
     * @param dimension the dimension that was migrated
     */
    public DimensionMigratedEvent(Dimension dimension) {
        this.dimension = dimension;
    }

    /** @return the dimension that was migrated */
    public Dimension getDimension() { return dimension; }

    /** @return the migrated dimension's name */
    public String getName() { return dimension.name(); }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
