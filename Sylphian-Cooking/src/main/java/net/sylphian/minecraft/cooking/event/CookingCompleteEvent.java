package net.sylphian.minecraft.cooking.event;

import net.sylphian.minecraft.cooking.quality.CookingQuality;
import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fired on the main thread when a station completes a recipe, before quality is rolled.
 * Listeners may add quality shifts ({@link #addQualityShift}), scale XP ({@link #multiplyXp}),
 * and set a bonus drop ({@link #setBonusOutput}); the service then rolls quality and places output.
 */
public final class CookingCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private final CookingRecipe recipe;
    private final UUID lastInteractor;

    private final Map<CookingQuality, Double> qualityShifts = new EnumMap<>(CookingQuality.class);
    private double xpMultiplier = 1.0;
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
     * Adds a weight delta for the given quality tier.
     * Positive values increase the chance of that tier; negative values decrease it.
     * Deltas from multiple listeners accumulate additively.
     *
     * @param tier  the tier to shift
     * @param delta the weight delta to add
     */
    public void addQualityShift(CookingQuality tier, double delta) {
        qualityShifts.merge(tier, delta, Double::sum);
    }

    /**
     * Returns all accumulated per-tier quality weight deltas.
     * The service passes these to {@link net.sylphian.minecraft.cooking.quality.QualityRoller}
     * after the event returns.
     *
     * @return unmodifiable view of quality shifts, keyed by tier
     */
    public Map<CookingQuality, Double> getQualityShifts() {
        return Collections.unmodifiableMap(qualityShifts);
    }

    /**
     * Multiplies the accumulated XP multiplier by the given factor.
     * The result is carried to the subsequent {@link CookingXpEvent}.
     *
     * @param factor the multiplier to apply, e.g. {@code 1.5} for +50% XP
     */
    public void multiplyXp(double factor) {
        xpMultiplier *= factor;
    }

    /**
     * Returns the accumulated XP multiplier from all listeners.
     * Starts at {@code 1.0}; each call to {@link #multiplyXp} compounds it.
     *
     * @return the final XP multiplier
     */
    public double getXpMultiplier() { return xpMultiplier; }

    /**
     * Sets a bonus item to be dropped naturally at the station block.
     * Only the last listener to call this wins.
     *
     * @param bonusOutput the item to drop, or null to clear
     */
    public void setBonusOutput(@Nullable ItemStack bonusOutput) { this.bonusOutput = bonusOutput; }

    /**
     * Returns the bonus item to be dropped naturally at the station, or null if none was set.
     *
     * @return bonus output, or null
     */
    public @Nullable ItemStack getBonusOutput() { return bonusOutput; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
