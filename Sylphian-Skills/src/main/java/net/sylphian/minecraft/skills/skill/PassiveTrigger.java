package net.sylphian.minecraft.skills.skill;

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
 */
public interface PassiveTrigger {}
