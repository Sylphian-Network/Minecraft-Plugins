package net.sylphian.minecraft.fishing.config;

import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * Immutable configuration for a single fish mutation.
 * Consolidates enabled state, base chance, and potion effects
 * into a single record rather than spreading them across multiple maps.
 *
 * @param enabled    whether the mutation is active
 * @param baseChance the base probability of the mutation occurring (0.0 to 1.0)
 * @param effects    the list of potion effects applied when the mutation triggers
 */
public record MutationConfig(boolean enabled, double baseChance, List<PotionEffect> effects) {
    /** Returns an empty disabled mutation config used as a safe default. */
    public static MutationConfig empty() {
        return new MutationConfig(false, 0.0, List.of());
    }
}