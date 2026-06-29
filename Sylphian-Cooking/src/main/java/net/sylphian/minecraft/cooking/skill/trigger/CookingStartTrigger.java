package net.sylphian.minecraft.cooking.skill.trigger;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.TraceEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trigger token fired when a cooking station starts a new recipe cycle.
 *
 * <p>Passives that reduce cook time write their fractional reductions into
 * this token via {@link #addReduction}. After all passives have fired,
 * the skill reads {@link #totalReduction()} and applies it to the
 * {@link net.sylphian.minecraft.cooking.event.CookingStartEvent}.</p>
 *
 * <p>Supports debug tracing via {@link #record}.</p>
 */
public final class CookingStartTrigger implements PassiveTrigger {

    /** The maximum combined reduction that may be applied to cook time. */
    public static final double MAX_REDUCTION = 0.75;

    private final CookingRecipe recipe;
    private double totalReduction = 0.0;
    private final List<TraceEntry> traceLog = new ArrayList<>();

    /**
     * @param recipe the recipe that just started cooking
     */
    public CookingStartTrigger(CookingRecipe recipe) {
        this.recipe = recipe;
    }

    /** @return the recipe that started cooking */
    public CookingRecipe recipe() { return recipe; }

    /**
     * Accumulates a fractional reduction contribution from a passive.
     *
     * @param fraction the reduction fraction to add, e.g. {@code 0.15} for 15%
     */
    public void addReduction(double fraction) {
        totalReduction += fraction;
    }

    /**
     * Returns the total accumulated cook-time reduction fraction before capping.
     * The service caps this at {@value #MAX_REDUCTION} when applying it.
     *
     * @return total reduction fraction
     */
    public double totalReduction() {
        return totalReduction;
    }

    @Override
    public void record(String source, String description, boolean active) {
        traceLog.add(new TraceEntry(source, description, active));
    }

    @Override
    public List<TraceEntry> traceEntries() {
        return Collections.unmodifiableList(traceLog);
    }
}
