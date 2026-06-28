package net.sylphian.minecraft.fishing.skill.ability;

import org.bukkit.Location;

/**
 * Immutable snapshot of a player's Steady Current momentum at a given location.
 *
 * @param stacks       number of consecutive same-spot catches (1 to max stacks)
 * @param lastLocation location of the most recent catch
 */
public record CatchMomentum(int stacks, Location lastLocation) {}
