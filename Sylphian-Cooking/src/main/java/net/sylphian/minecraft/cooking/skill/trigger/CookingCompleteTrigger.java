package net.sylphian.minecraft.cooking.skill.trigger;

import net.sylphian.minecraft.cooking.quality.CookingQuality;
import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.TraceEntry;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Trigger token fired when a cooking station completes a recipe.
 * Passives accumulate an XP multiplier via {@link #multiplyXp}, quality weight
 * shifts via {@link #shiftQuality}, and an optional bonus drop via {@link #setBonusOutput}.
 */
public final class CookingCompleteTrigger implements PassiveTrigger {

    private final CookingRecipe recipe;
    private double xpMultiplier = 1.0;
    private final Map<CookingQuality, Double> qualityShifts = new EnumMap<>(CookingQuality.class);
    private @Nullable ItemStack bonusOutput;
    private boolean preserveIngredient;
    private final List<TraceEntry> traceLog = new ArrayList<>();

    /**
     * @param recipe the recipe that just completed
     */
    public CookingCompleteTrigger(CookingRecipe recipe) {
        this.recipe = recipe;
    }

    /** @return the recipe that completed */
    public CookingRecipe recipe() { return recipe; }

    /** Marks that one matched ingredient should not be consumed by this cook. */
    public void preserveIngredient() { this.preserveIngredient = true; }

    /** @return true if one ingredient should be preserved this cook */
    public boolean shouldPreserveIngredient() { return preserveIngredient; }

    /**
     * Multiplies the accumulated XP multiplier by the given factor.
     *
     * @param factor the multiplier to apply, e.g. {@code 1.5} for +50% XP
     */
    public void multiplyXp(double factor) {
        xpMultiplier *= factor;
    }

    /**
     * Returns the combined XP multiplier accumulated from all passives.
     * Starts at {@code 1.0}; each call to {@link #multiplyXp} compounds it.
     *
     * @return the final XP multiplier
     */
    public double xpMultiplier() { return xpMultiplier; }

    /**
     * Sets a bonus output item to be dropped at the station.
     * Only the last passive to call this wins; earlier ones are overwritten.
     *
     * @param item the item to drop as a bonus, or null to clear
     */
    public void setBonusOutput(@Nullable ItemStack item) { this.bonusOutput = item; }

    /**
     * Adds a weight delta to the given quality tier's probability.
     * Positive values increase the chance of that tier; negative values decrease it.
     * Deltas from multiple passives accumulate additively.
     * The roller clamps each tier at zero so negative totals cannot invert weights.
     *
     * @param tier  the tier to shift
     * @param delta the weight delta to add
     */
    public void shiftQuality(CookingQuality tier, double delta) {
        qualityShifts.merge(tier, delta, Double::sum);
    }

    /**
     * Returns the accumulated per-tier weight deltas contributed by all passives.
     * The returned map is unmodifiable.
     *
     * @return quality weight shifts, keyed by tier
     */
    public Map<CookingQuality, Double> qualityShifts() {
        return Collections.unmodifiableMap(qualityShifts);
    }

    /** @return the bonus output item, or null if none was set */
    public @Nullable ItemStack bonusOutput() { return bonusOutput; }

    @Override
    public void record(String source, String description, boolean active) {
        traceLog.add(new TraceEntry(source, description, active));
    }

    @Override
    public List<TraceEntry> traceEntries() {
        return Collections.unmodifiableList(traceLog);
    }
}
