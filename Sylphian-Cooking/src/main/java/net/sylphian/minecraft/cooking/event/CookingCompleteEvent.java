package net.sylphian.minecraft.cooking.event;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Fired on the main thread when a cooking station completes a recipe, before
 * the output is committed to the station state.
 *
 * <p>Listeners (e.g. the cooking skill) may set a {@link #bonusOutput} item
 * to be dropped naturally at the station block when the cycle finishes.
 * XP is also awarded by the skill listener reading this event.</p>
 *
 * <p>The {@link #lastInteractor} is the player who last mutated an ingredient
 * or fuel slot, and is the player who receives skill XP and bonus effects.</p>
 */
public final class CookingCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private final CookingRecipe recipe;
    private final UUID lastInteractor;
    private @Nullable ItemStack bonusOutput;

    /**
     * @param location       the location of the cooking station block
     * @param recipe         the recipe that just completed
     * @param lastInteractor UUID of the player who last modified the station
     */
    public CookingCompleteEvent(Location location, CookingRecipe recipe, UUID lastInteractor) {
        this.location = location;
        this.recipe = recipe;
        this.lastInteractor = lastInteractor;
    }

    /** @return the location of the cooking station block */
    public Location getLocation() { return location; }

    /** @return the recipe that completed */
    public CookingRecipe getRecipe() { return recipe; }

    /** @return the UUID of the player who last modified the station */
    public UUID getLastInteractor() { return lastInteractor; }

    /**
     * Returns the bonus output item to be dropped at the station, or null if none.
     *
     * @return bonus output, or null
     */
    public @Nullable ItemStack getBonusOutput() { return bonusOutput; }

    /**
     * Sets a bonus item to be dropped naturally at the station block.
     * Called by the cooking skill listener when a bonus yield passive triggers.
     *
     * @param bonusOutput the item to drop, or null to clear
     */
    public void setBonusOutput(@Nullable ItemStack bonusOutput) { this.bonusOutput = bonusOutput; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
