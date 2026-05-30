package net.sylphian.minecraft.fishing.weather;

import org.bukkit.World;

public enum WeatherCondition {
    CLEAR,
    RAIN,
    THUNDERSTORM;

    public static WeatherCondition from(World world) {
        if (world.isThundering()) return THUNDERSTORM;
        if (world.hasStorm()) return RAIN;
        return CLEAR;
    }
}