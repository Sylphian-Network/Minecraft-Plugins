package net.sylphian.minecraft.dimensions.spawn;

import org.bukkit.World;

/**
 * The time of day during which a spawn entry is eligible.
 */
public enum TimeWindow {

    /** Eligible only while it is daytime in the world. */
    DAY,
    /** Eligible only while it is night in the world. */
    NIGHT,
    /** Eligible at any time. */
    ANY;

    /**
     * Returns whether this window is currently open in the given world.
     *
     * @param world the world to check
     * @return true if a spawn is allowed now
     */
    public boolean isOpen(World world) {
        return switch (this) {
            case DAY -> world.isDayTime();
            case NIGHT -> !world.isDayTime();
            case ANY -> true;
        };
    }
}
