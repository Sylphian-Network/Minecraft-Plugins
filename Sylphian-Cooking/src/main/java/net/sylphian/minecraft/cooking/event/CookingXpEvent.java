package net.sylphian.minecraft.cooking.event;

import net.sylphian.minecraft.cooking.quality.CookingQuality;
import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired on the main thread after quality has been rolled for a completed recipe.
 *
 * <p>Sylphian-Skills listens to this event to award XP. The {@link #xpMultiplier}
 * reflects any passive bonuses accumulated during the preceding
 * {@link CookingCompleteEvent}.</p>
 */
public final class CookingXpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final CookingRecipe recipe;
    private final CookingQuality quality;
    private final double xpMultiplier;

    /**
     * @param playerUuid    the UUID of the player who last interacted with the station
     * @param recipe        the recipe that completed
     * @param quality       the quality tier that was rolled
     * @param xpMultiplier  the combined passive XP multiplier from {@link CookingCompleteEvent}
     */
    public CookingXpEvent(UUID playerUuid, CookingRecipe recipe, CookingQuality quality, double xpMultiplier) {
        this.playerUuid   = playerUuid;
        this.recipe       = recipe;
        this.quality      = quality;
        this.xpMultiplier = xpMultiplier;
    }

    /** @return the UUID of the player who last interacted with the station */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return the recipe that completed */
    public CookingRecipe getRecipe() { return recipe; }

    /** @return the quality tier that was rolled */
    public CookingQuality getQuality() { return quality; }

    /**
     * Returns the combined passive XP multiplier.
     * Starts at {@code 1.0}; increased by passive abilities via {@link CookingCompleteEvent#multiplyXp}.
     *
     * @return the XP multiplier
     */
    public double getXpMultiplier() { return xpMultiplier; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
