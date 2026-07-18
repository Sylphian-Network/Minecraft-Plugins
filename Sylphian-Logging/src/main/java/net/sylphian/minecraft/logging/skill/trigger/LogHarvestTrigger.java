package net.sylphian.minecraft.logging.skill.trigger;

import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.TraceEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trigger token fired when a logging node is about to be harvested. Wraps the
 * cancellable {@link NodeHarvestEvent}; passives mutate its accumulators
 * through {@link #event()}.
 */
public final class LogHarvestTrigger implements PassiveTrigger {

    private final NodeHarvestEvent event;
    private final List<TraceEntry> traceLog = new ArrayList<>();

    public LogHarvestTrigger(NodeHarvestEvent event) {
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
