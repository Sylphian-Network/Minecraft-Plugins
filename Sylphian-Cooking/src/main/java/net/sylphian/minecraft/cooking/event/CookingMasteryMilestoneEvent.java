package net.sylphian.minecraft.cooking.event;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired on the main thread when a player's cook count for a recipe reaches a configured mastery
 * milestone (e.g. 50, 100). Read from the database count, so each milestone fires exactly once
 * per player per recipe.
 */
public final class CookingMasteryMilestoneEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final CookingRecipe recipe;
    private final int milestone;

    /**
     * @param playerUuid the UUID of the player who reached the milestone
     * @param recipe     the recipe whose milestone was reached
     * @param milestone  the cook count milestone that was reached (e.g. 50 or 100)
     */
    public CookingMasteryMilestoneEvent(UUID playerUuid, CookingRecipe recipe, int milestone) {
        this.playerUuid = playerUuid;
        this.recipe = recipe;
        this.milestone = milestone;
    }

    /** @return the UUID of the player who reached the milestone */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return the recipe whose milestone was reached */
    public CookingRecipe getRecipe() { return recipe; }

    /** @return the cook count milestone that was reached (e.g. 50 or 100) */
    public int getMilestone() { return milestone; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
