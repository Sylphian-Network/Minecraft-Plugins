package net.sylphian.minecraft.mining.skill;

/**
 * Immutable snapshot of a player's Steady Rhythm streak.
 *
 * @param stacks         consecutive in-window mining harvests, capped by config
 * @param lastMillis     epoch millis of the most recent mining harvest
 */
public record MiningStreak(int stacks, long lastMillis) {}
