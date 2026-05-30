package net.sylphian.minecraft.fishing.weather;

import org.bukkit.World;

/**
 * Represents the weather condition at the time of a fishing catch.
 * Used by LootManager to apply rarity chance multipliers.
 */
public enum WeatherCondition {
    /** No precipitation. Default multipliers apply. */
    CLEAR,
    /** Standard rainfall. Increases rare catch chances moderately. */
    RAIN,
    /** Thunderstorm. Significantly increases rare and legendary catch chances. */
    THUNDERSTORM;

    /**
     * Determines the current weather condition in the specified world.
     *
     * @param world the world to check
     * @return the current WeatherCondition
     */
    public static WeatherCondition from(World world) {
        if (world.isThundering()) return THUNDERSTORM;
        if (world.hasStorm()) return RAIN;
        return CLEAR;
    }
}