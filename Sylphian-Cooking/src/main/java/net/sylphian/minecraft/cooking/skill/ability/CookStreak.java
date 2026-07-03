package net.sylphian.minecraft.cooking.skill.ability;

import org.bukkit.Location;

/**
 * Immutable snapshot of a player's Seasoned Hands streak.
 *
 * @param stacks         number of consecutive cooks (1 to max stacks)
 * @param lastLocation   location of the most recent cook
 * @param lastCookMillis epoch millis of the most recent cook
 */
public record CookStreak(int stacks, Location lastLocation, long lastCookMillis) {}
