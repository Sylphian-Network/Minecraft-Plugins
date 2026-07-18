package net.sylphian.minecraft.mining.skill.trigger;

import net.sylphian.minecraft.gathering.event.NodeHarvestedEvent;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.TraceEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trigger token fired after a mining node has been harvested, for passives that
 * only observe the outcome (e.g. the Steady Rhythm streak tracker). Wraps the
 * non-cancellable {@link NodeHarvestedEvent}.
 */
public final class OreHarvestedTrigger implements PassiveTrigger {

    private final NodeHarvestedEvent event;
    private final List<TraceEntry> traceLog = new ArrayList<>();

    public OreHarvestedTrigger(NodeHarvestedEvent event) {
        this.event = event;
    }

    /** @return the underlying harvested event */
    public NodeHarvestedEvent event() {
        return event;
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
