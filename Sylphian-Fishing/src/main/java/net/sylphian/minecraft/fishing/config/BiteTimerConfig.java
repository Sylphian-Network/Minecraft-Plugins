package net.sylphian.minecraft.fishing.config;

import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;

import java.util.Map;

/**
 * Immutable configuration for the custom bite timer system.
 *
 * <p>Controls how long a player waits for a fish to bite based on
 * the rolled rarity and current weather condition.</p>
 *
 * @param baseMin         minimum bite delay in ticks
 * @param baseMax         maximum bite delay in ticks
 * @param rarityModifiers multipliers per rarity — higher rarity = longer wait
 * @param weatherModifiers multipliers per weather — rain reduces wait time
 */
public record BiteTimerConfig(int baseMin, int baseMax, Map<Rarity, Double> rarityModifiers, Map<WeatherCondition, Double> weatherModifiers) {
    /**
     * Calculates the final bite delay in ticks for the given rarity and weather.
     * The base range is scaled by both modifiers and a random value is picked
     * within the resulting range.
     *
     * @param rarity  the rolled rarity for this catch
     * @param weather the current weather condition
     * @param random  the random source
     * @return the calculated bite delay in ticks
     */
    public int calculate(Rarity rarity, WeatherCondition weather, java.util.Random random) {
        double rarityMod = rarityModifiers.getOrDefault(rarity, 1.0);
        double weatherMod = weatherModifiers.getOrDefault(weather, 1.0);

        int min = (int) (baseMin * rarityMod * weatherMod);
        int max = (int) (baseMax * rarityMod * weatherMod);

        min = Math.max(20, min);   // Never less than 1 second
        max = Math.max(min + 20, max);

        return min + random.nextInt(max - min);
    }
}