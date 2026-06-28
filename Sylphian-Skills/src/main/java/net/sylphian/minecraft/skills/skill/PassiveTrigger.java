package net.sylphian.minecraft.skills.skill;

import java.util.List;

/**
 * Marker interface for passive ability trigger tokens.
 *
 * <p>Each skill module defines its own concrete trigger types (e.g.
 * {@code FishCastTrigger}, {@code FishCatchTrigger}). Trigger tokens serve two
 * purposes: they carry the event context that passives need to act on, and they
 * act as accumulators for aggregated outputs (e.g. a combined timer reduction
 * or XP multiplier) that the skill applies after all passives have fired.</p>
 *
 * <p>Trigger types must be defined in the owning skill module, not in
 * Sylphian-Skills, because they reference domain classes specific to that
 * module.</p>
 *
 * <p>Tokens may optionally support debug tracing by overriding {@link #record}
 * and {@link #traceEntries}. Passives call {@link #record} after contributing
 * to the token; the skill reads {@link #traceEntries} after
 * {@link AbstractSkill#firePassives} returns and forwards the log to any
 * registered debug watcher.</p>
 */
public interface PassiveTrigger {

    /**
     * Records an ability's contribution to this trigger for debug tracing.
     * No-op by default; override in concrete trigger classes that support tracing.
     *
     * @param source      display name of the contributing ability
     * @param description short human-readable description of what the ability did
     * @param active      {@code true} if the contribution came from an active ability
     */
    default void record(String source, String description, boolean active) {}

    /**
     * Convenience overload for passive contributions; delegates to
     * {@link #record(String, String, boolean)} with {@code active = false}.
     *
     * @param source      display name of the contributing ability
     * @param description short human-readable description of what the ability did
     */
    default void record(String source, String description) {
        record(source, description, false);
    }

    /**
     * Returns all trace entries recorded on this trigger, in the order they were added.
     * Returns an empty list by default.
     *
     * @return recorded contributions, or an empty list if tracing is not supported
     */
    default List<TraceEntry> traceEntries() {
        return List.of();
    }
}
