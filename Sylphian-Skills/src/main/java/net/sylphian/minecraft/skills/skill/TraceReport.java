package net.sylphian.minecraft.skills.skill;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured description of one skill-trace block, rendered uniformly by
 * {@link AbstractSkill#sendTrace(java.util.UUID, TraceReport)} so every skill's
 * trace shares the same layout: a header, the ability contribution lines, then
 * zero or more labelled result lines.
 */
public final class TraceReport {

    /** A labelled result line shown beneath the ability contributions. */
    public record Result(String label, String value) {}

    private final String color;
    private final String event;
    private String subject = "";
    private int level = -1;
    private @Nullable String context;
    private List<TraceEntry> entries = List.of();
    private boolean hasEntrySection;
    private final List<Result> results = new ArrayList<>();

    private TraceReport(String color, String event) {
        this.color = color;
        this.event = event;
    }

    /**
     * @param color a MiniMessage colour tag for the event label, e.g. {@code "<dark_aqua>"}
     * @param event the event name, e.g. {@code "Cast"} or {@code "Cook Complete"}
     * @return a new report builder
     */
    public static TraceReport of(String color, String event) {
        return new TraceReport(color, event);
    }

    /** @param subject the player name shown in the header */
    public TraceReport subject(String subject) { this.subject = subject; return this; }

    /** @param level the skill level shown in the header; negative to omit */
    public TraceReport level(int level) { this.level = level; return this; }

    /** @param context an optional MiniMessage detail shown after the level, or null */
    public TraceReport context(@Nullable String context) { this.context = context; return this; }

    /** @param entries the ability contributions recorded on the trigger */
    public TraceReport entries(List<TraceEntry> entries) {
        this.entries = entries;
        this.hasEntrySection = true;
        return this;
    }

    /**
     * Adds a labelled result line.
     *
     * @param label the result label, e.g. {@code "Result"} or {@code "XP"}
     * @param value the MiniMessage value string
     * @return this builder
     */
    public TraceReport result(String label, String value) {
        this.results.add(new Result(label, value));
        return this;
    }

    String color()              { return color; }
    String event()              { return event; }
    String subject()            { return subject; }
    int level()                 { return level; }
    @Nullable String context()  { return context; }
    List<TraceEntry> entries()  { return entries; }
    boolean hasEntrySection()   { return hasEntrySection; }
    List<Result> results()      { return results; }
}
