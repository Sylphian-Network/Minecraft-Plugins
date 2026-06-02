package net.sylphian.minecraft.fishing.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable configuration for a single bait type.
 *
 * <p>Defines the bait's visual appearance, zone behaviour, and the fishing
 * bonuses applied to any player whose hook lands within the zone.</p>
 *
 * @param id                  unique identifier matching the key in baits.yml
 * @param displayName         MiniMessage formatted name shown on the item and text display
 * @param material            the item material used for the throwable bait item
 * @param radius              zone radius in blocks (2D, ignores Y)
 * @param durationSeconds     how long the zone persists after landing
 * @param particle            Bukkit Particle enum name rendered on the zone ring
 * @param biteTimerMultiplier multiplier applied to the calculated bite delay (below 1.0 = faster)
 * @param rarityMultipliers   per-rarity catch chance multipliers applied inside the zone
 */
public record BaitConfig(
        String id,
        String displayName,
        Material material,
        int radius,
        int durationSeconds,
        String particle,
        double biteTimerMultiplier,
        Map<String, Double> rarityMultipliers
) {

    /**
     * Parses a BaitConfig from a configuration section.
     *
     * @param id  the bait identifier (the YAML key)
     * @param sec the configuration section for this bait
     * @return the parsed BaitConfig, or null if the section is null
     */
    public static BaitConfig fromSection(String id, ConfigurationSection sec) {
        if (sec == null) return null;

        Material material;
        try {
            material = Material.valueOf(sec.getString("material", "SLIME_BALL").toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.SLIME_BALL;
        }

        Map<String, Double> rarityMultipliers = new HashMap<>();
        ConfigurationSection raritySection = sec.getConfigurationSection("rarity-multipliers");
        if (raritySection != null) {
            for (String key : raritySection.getKeys(false)) {
                rarityMultipliers.put(key.toUpperCase(), raritySection.getDouble(key, 1.0));
            }
        }

        return new BaitConfig(
                id,
                sec.getString("display-name", "<white>" + id),
                material,
                sec.getInt("radius", 8),
                sec.getInt("duration", 300),
                sec.getString("particle", "BUBBLE"),
                sec.getDouble("bite-timer-multiplier", 1.0),
                rarityMultipliers
        );
    }
}
