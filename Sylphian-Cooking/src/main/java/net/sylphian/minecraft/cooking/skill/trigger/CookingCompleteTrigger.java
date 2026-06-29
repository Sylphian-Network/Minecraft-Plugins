package net.sylphian.minecraft.cooking.skill.trigger;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.TraceEntry;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trigger token fired when a cooking station completes a recipe.
 *
 * <p>Passives may multiply the XP award via {@link #multiplyXp} and set a
 * bonus output item via {@link #setBonusOutput}. After all passives have fired,
 * the skill reads these values and applies them to the
 * {@link net.sylphian.minecraft.cooking.event.CookingCompleteEvent}.</p>
 *
 * <p>Supports debug tracing via {@link #record}.</p>
 */
public final class CookingCompleteTrigger implements PassiveTrigger {

    private final CookingRecipe recipe;
    private double xpMultiplier = 1.0;
    private @Nullable ItemStack bonusOutput;
    private final List<TraceEntry> traceLog = new ArrayList<>();

    /**
     * @param recipe the recipe that just completed
     */
    public CookingCompleteTrigger(CookingRecipe recipe) {
        this.recipe = recipe;
    }

    /** @return the recipe that completed */
    public CookingRecipe recipe() { return recipe; }

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
