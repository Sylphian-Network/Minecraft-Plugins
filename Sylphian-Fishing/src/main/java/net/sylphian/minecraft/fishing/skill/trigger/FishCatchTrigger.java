package net.sylphian.minecraft.fishing.skill.trigger;

import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.TraceEntry;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trigger token fired when a player catches a Sylphian fish (state CAUGHT_FISH).
 *
 * <p>Passives that multiply XP write their factors into this token via
 * {@link #multiplyXp}. After all passives have fired, the skill reads
 * {@link #xpMultiplier()} to compute the final XP award.</p>
 *
 * <p>Supports debug tracing: passives call {@link #record} after contributing,
 * and the skill reads {@link #traceEntries()} to send the log to any watcher.</p>
 */
public final class FishCatchTrigger implements PassiveTrigger {

    private final ItemStack caught;
    private final Location  location;
    private double xpMultiplier = 1.0;
    private final List<TraceEntry> traceLog = new ArrayList<>();

    /**
     * @param caught   the item stack that was caught
     * @param location the location of the caught item entity
     */
    public FishCatchTrigger(ItemStack caught, Location location) {
        this.caught   = caught;
        this.location = location;
    }

    /** @return the item stack that was caught */
    public ItemStack caught() {
        return caught;
    }

    /** @return the location of the catch */
    public Location location() {
        return location;
    }

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
     * @return the final XP multiplier to apply to the base XP value
     */
    public double xpMultiplier() {
        return xpMultiplier;
    }

    @Override
    public void record(String source, String description) {
        traceLog.add(new TraceEntry(source, description));
    }

    @Override
    public void recordActive(String source, String description) {
        traceLog.add(new TraceEntry(source, description, true));
    }

    @Override
    public List<TraceEntry> traceEntries() {
        return Collections.unmodifiableList(traceLog);
    }
}
