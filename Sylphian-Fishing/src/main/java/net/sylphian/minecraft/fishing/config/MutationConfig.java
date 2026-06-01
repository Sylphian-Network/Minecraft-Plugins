package net.sylphian.minecraft.fishing.config;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Parses a MutationConfig from a configuration section.
     *
     * @param sec the configuration section for a single mutation
     * @return the parsed MutationConfig, or {@link #empty()} if the section is null
     */
    public static MutationConfig fromSection(ConfigurationSection sec) {
        if (sec == null) return empty();
        return new MutationConfig(
                sec.getBoolean("enabled", false),
                sec.getDouble("base-chance", 0.0),
                parseEffects(sec.getList("effects"))
        );
    }

    private static List<PotionEffect> parseEffects(List<?> list) {
        List<PotionEffect> effects = new ArrayList<>();
        if (list == null) return effects;

        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> map)) continue;

            String typeName = (String) map.get("effect");
            if (typeName == null) continue;

            int duration = map.get("duration") instanceof Number n ? n.intValue() : 200;
            int amplifier = map.get("amplifier") instanceof Number n ? n.intValue() : 0;

            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(
                    NamespacedKey.minecraft(typeName.toLowerCase()));
            if (type != null) effects.add(new PotionEffect(type, duration, amplifier));
        }
        return effects;
    }
}