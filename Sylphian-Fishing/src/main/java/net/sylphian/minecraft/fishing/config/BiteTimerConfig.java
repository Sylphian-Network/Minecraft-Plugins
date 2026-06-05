package net.sylphian.minecraft.fishing.config;

import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;

import java.util.Map;
import java.util.Random;

/**
 * Immutable configuration for the custom fishing timer system.
 *
 * <p>Controls how long a player waits for a fish to appear ({@link #calculate})
 * and how long it nibbles before biting ({@link #calculateLureTime}),
 * both driven by the pre-rolled rarity. Wait time is also affected by weather;
 * lure time is not.</p>
 *
 * @param baseMin              minimum wait delay in ticks before a fish appears
 * @param baseMax              maximum wait delay in ticks before a fish appears
 * @param rarityModifiers      wait time multipliers per rarity — higher rarity = longer wait
 * @param weatherModifiers     wait time multipliers per weather — rain reduces wait time
 * @param lureBaseMin          minimum lure time in ticks before the fish bites
 * @param lureBaseMax          maximum lure time in ticks before the fish bites
 * @param lureRarityModifiers  lure time multipliers per rarity — higher rarity = longer nibble
 */
public record BiteTimerConfig(
        int baseMin, int baseMax,
        Map<Rarity, Double> rarityModifiers,
        Map<WeatherCondition, Double> weatherModifiers,
        int lureBaseMin, int lureBaseMax,
        Map<Rarity, Double> lureRarityModifiers) {

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
    public int calculate(Rarity rarity, WeatherCondition weather, Random random) {
        double rarityMod = rarityModifiers.getOrDefault(rarity, 1.0);
        double weatherMod = weatherModifiers.getOrDefault(weather, 1.0);

        int min = (int) (baseMin * rarityMod * weatherMod);
        int max = (int) (baseMax * rarityMod * weatherMod);

        min = Math.max(20, min);   // Never less than 1 second
        max = Math.max(min + 20, max);

        return min + random.nextInt(max - min);
    }

    /**
     * Calculates the lure time in ticks for the given rarity.
     * Lure time controls how long the fish nibbles before biting once it appears.
     * Weather does not influence this phase.
     *
     * @param rarity the rolled rarity for this catch
     * @param random the random source
     * @return the calculated lure time in ticks
     */
    public int calculateLureTime(Rarity rarity, Random random) {
        double rarityMod = lureRarityModifiers.getOrDefault(rarity, 1.0);

        int min = (int) (lureBaseMin * rarityMod);
        int max = (int) (lureBaseMax * rarityMod);

        min = Math.max(1, min);
        max = Math.max(min + 1, max);

        return min + random.nextInt(max - min);
    }
}