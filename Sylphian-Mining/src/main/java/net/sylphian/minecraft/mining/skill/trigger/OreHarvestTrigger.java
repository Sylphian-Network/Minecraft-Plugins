package net.sylphian.minecraft.mining.skill.trigger;

import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.TraceEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trigger token fired when a mining node is about to be harvested. Wraps the
 * cancellable {@link NodeHarvestEvent}; passives mutate its accumulators
 * (yield, XP, bonus loot, deplete) through {@link #event()}.
 */
public final class OreHarvestTrigger implements PassiveTrigger {

    private final NodeHarvestEvent event;
    private final List<TraceEntry> traceLog = new ArrayList<>();

    public OreHarvestTrigger(NodeHarvestEvent event) {
        this.event = event;
    }

    /** @return the underlying harvest event whose accumulators passives mutate */
    public NodeHarvestEvent event() {
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
