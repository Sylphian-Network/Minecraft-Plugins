package net.sylphian.minecraft.cooking.event;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired on the main thread at the start of the first tick of a new cook cycle:
 * when a station transitions from no active recipe to a matching one.
 *
 * <p>Listeners (e.g. the cooking skill) may reduce {@link #effectiveCookTime}
 * to apply passive cook-speed bonuses. The service stores the result on the
 * station state and uses it for the rest of that cycle.</p>
 *
 * <p>The {@link #lastInteractor} is the player who last mutated an ingredient
 * or fuel slot, and is the player whose skill levels are applied.</p>
 */
public final class CookingStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private final CookingRecipe recipe;
    private final UUID lastInteractor;
    private int effectiveCookTime;

    /**
     * @param location         the location of the cooking station block
     * @param recipe           the recipe that just started cooking
     * @param lastInteractor   UUID of the player who last modified the station
     * @param baseCookTime     the recipe's unmodified cook time in ticks
     */
    public CookingStartEvent(Location location, CookingRecipe recipe, UUID lastInteractor, int baseCookTime) {
        this.location = location;
        this.recipe = recipe;
        this.lastInteractor = lastInteractor;
        this.effectiveCookTime = baseCookTime;
    }

    /** @return the location of the cooking station block */
    public Location getLocation() { return location; }

    /** @return the recipe that started cooking */
    public CookingRecipe getRecipe() { return recipe; }

    /** @return the UUID of the player who last modified the station */
    public UUID getLastInteractor() { return lastInteractor; }

    /**
     * Returns the effective cook time in ticks for this cycle.
     * Starts at the recipe's base cook time; listeners may reduce it.
     *
     * @return effective cook time in ticks
     */
    public int getEffectiveCookTime() { return effectiveCookTime; }

    /**
     * Sets the effective cook time for this cycle.
     * Must be at least 20 ticks (1 second); the service clamps automatically.
     *
     * @param effectiveCookTime cook time in ticks
     */
    public void setEffectiveCookTime(int effectiveCookTime) { this.effectiveCookTime = effectiveCookTime; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
