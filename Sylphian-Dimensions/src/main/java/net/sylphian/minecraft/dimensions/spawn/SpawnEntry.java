package net.sylphian.minecraft.dimensions.spawn;

import org.bukkit.World;

/**
 * One row of a dimension's spawn table: a custom entity to spawn plus the
 * conditions under which it may appear.
 *
 * @param entityId  the namespaced entity reference, e.g. {@code "sylphian-entities:rift_zombie"}
 * @param weight    the relative weight when picking among eligible entries; higher is more common
 * @param time      the time-of-day window this entry is eligible in
 * @param minGroup  the minimum pack size, inclusive
 * @param maxGroup  the maximum pack size, inclusive
 * @param minLight  the minimum block (artificial) light at the spawn block, inclusive
 * @param maxLight  the maximum block (artificial) light at the spawn block, inclusive
 * @param minY      the minimum spawn Y, inclusive
 * @param maxY      the maximum spawn Y, inclusive
 */
public record SpawnEntry(
        String entityId,
        int weight,
        TimeWindow time,
        int minGroup,
        int maxGroup,
        int minLight,
        int maxLight,
        int minY,
        int maxY) {

    /**
     * Returns whether this entry may spawn at the given position and light right now.
     *
     * @param world the world being spawned in
     * @param y     the candidate spawn Y
     * @param light the light level at the candidate block
     * @return true if every condition is satisfied
     */
    public boolean matches(World world, int y, int light) {
        return time.isOpen(world)
                && y >= minY && y <= maxY
                && light >= minLight && light <= maxLight;
    }
}
