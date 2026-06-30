package net.sylphian.minecraft.cooking.event;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired on the main thread the first time a player completes a recipe (cook count 0 to 1).
 * Detected from the database count, so it fires exactly once per player per recipe.
 */
public final class CookingDiscoveryEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final CookingRecipe recipe;

    /**
     * @param playerUuid the UUID of the player who discovered the recipe
     * @param recipe     the recipe that was completed for the first time
     */
    public CookingDiscoveryEvent(UUID playerUuid, CookingRecipe recipe) {
        this.playerUuid = playerUuid;
        this.recipe = recipe;
    }

    /** @return the UUID of the player who discovered the recipe */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return the recipe that was completed for the first time */
    public CookingRecipe getRecipe() { return recipe; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
