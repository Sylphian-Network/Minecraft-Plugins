package net.sylphian.minecraft.fishing.skill.trigger;

import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.TraceEntry;
import org.bukkit.entity.FishHook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trigger token fired when a player casts their fishing rod (state FISHING).
 *
 * <p>Passives that reduce hook wait times write their fractional reductions into
 * this token via {@link #addReduction}. After all passives have fired,
 * call {@link #applyToHook()} to apply the combined reduction to the hook in a
 * single pass, capped at 90%.</p>
 *
 * <p>Supports debug tracing: passives call {@link #record} after contributing,
 * and the skill reads {@link #traceEntries()} to send the log to any watcher.</p>
 */
public final class FishCastTrigger implements PassiveTrigger {

    /** The maximum combined reduction applied to hook wait times. */
    private static final double MAX_REDUCTION = 0.90;

    /** Minimum wait time in ticks after reduction is applied. */
    private static final int MIN_WAIT_TICKS = 20;

    private final FishHook hook;
    private double totalReduction = 0.0;
    private final List<TraceEntry> traceLog = new ArrayList<>();

    /**
     * @param hook the hook entity for this cast
     */
    public FishCastTrigger(FishHook hook) {
        this.hook = hook;
    }

    /** @return the hook entity for this cast */
    public FishHook hook() {
        return hook;
    }

    /**
     * Accumulates a fractional reduction contribution from a passive.
     *
     * @param fraction the reduction fraction to add, e.g. {@code 0.15} for 15%
     */
    public void addReduction(double fraction) {
        totalReduction += fraction;
    }

    /**
     * Returns the total accumulated cast timer reduction fraction before capping.
     *
     * @return total reduction fraction; cap is applied separately in {@link #applyToHook()}
     */
    public double totalReduction() {
        return totalReduction;
    }

    /**
     * Applies the accumulated reduction to the hook's wait times.
     * The combined reduction is capped at {@value #MAX_REDUCTION} (90%).
     * Does nothing if the total reduction is zero or negative.
     */
    public void applyToHook() {
        if (totalReduction <= 0.0) return;
        double capped = Math.min(MAX_REDUCTION, totalReduction);
        int newMin = Math.max(MIN_WAIT_TICKS, (int) (hook.getMinWaitTime() * (1.0 - capped)));
        int newMax = Math.max(newMin, (int) (hook.getMaxWaitTime() * (1.0 - capped)));
        hook.setMinWaitTime(newMin);
        hook.setMaxWaitTime(newMax);
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
