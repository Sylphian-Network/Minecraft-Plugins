package net.sylphian.minecraft.logging.skill;

/**
 * Immutable snapshot of a player's Woodsman's Rhythm streak.
 *
 * @param stacks     consecutive in-window logging harvests, capped by config
 * @param lastMillis epoch millis of the most recent logging harvest
 */
public record LoggingStreak(int stacks, long lastMillis) {}
