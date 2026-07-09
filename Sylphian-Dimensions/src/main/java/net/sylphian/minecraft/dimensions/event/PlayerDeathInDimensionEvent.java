package net.sylphian.minecraft.dimensions.event;

import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.model.DimensionRuleset;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread when a player dies inside a managed dimension,
 * carrying the resolved dimension and its ruleset. Not cancellable.
 */
public class PlayerDeathInDimensionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final Dimension dimension;

    /**
     * @param playerUuid the player who died
     * @param dimension  the dimension they died in
     */
    public PlayerDeathInDimensionEvent(UUID playerUuid, Dimension dimension) {
        this.playerUuid = playerUuid;
        this.dimension = dimension;
    }

    /** @return the player who died */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return the dimension the player died in */
    public Dimension getDimension() { return dimension; }

    /** @return the ruleset of the dimension the player died in */
    public DimensionRuleset getRuleset() { return dimension.ruleset(); }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
