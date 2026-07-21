package net.sylphian.minecraft.fishing.config;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
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
 * @param particleData        resolved data object for particles that require it (DustOptions, Color,
 *                            BlockData, etc.); null for data-free particles or unsupported types
 * @param biteTimerMultiplier multiplier applied to the calculated bite delay (below 1.0 = faster)
 * @param mutationChanceMultiplier multiplier applied to all mutation base chances inside the zone
 * @param rarityMultipliers   per-rarity catch chance multipliers applied inside the zone
 */
public record BaitConfig(
        String id,
        String displayName,
        Material material,
        int radius,
        int durationSeconds,
        String particle,
        @Nullable Object particleData,
        double biteTimerMultiplier,
        double mutationChanceMultiplier,
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

        String particleName = sec.getString("particle", "BUBBLE");
        Object particleData = null;
        try {
            Particle particleEnum = Particle.valueOf(particleName.toUpperCase());
            particleData = parseParticleData(particleEnum, sec.getConfigurationSection("particle-data"));
        } catch (IllegalArgumentException ignored) {
            // Unknown particle name: fall back to no particle rather than failing the bait load.
        }

        return new BaitConfig(
                id,
                sec.getString("display-name", "<white>" + id),
                material,
                sec.getInt("radius", 8),
                sec.getInt("duration", 300),
                particleName,
                particleData,
                sec.getDouble("bite-timer-multiplier", 1.0),
                sec.getDouble("mutation-chance-multiplier", 1.0),
                rarityMultipliers
        );
    }

    /**
     * Resolves the particle data object for the given particle type from the config section.
     * Returns null for particles that require no data, or for unsupported data types
     * (ITEM, VIBRATION, TRAIL, SPELL) where meaningful zone ring config is not possible.
     *
     * @param particle the particle type to resolve data for
     * @param data     the {@code particle-data} config section, or null if absent
     * @return the resolved data object, or null
     */
    @Nullable
    private static Object parseParticleData(Particle particle, @Nullable ConfigurationSection data) {
        Class<?> type = particle.getDataType();
        if (type == Void.class || data == null) return null;

        if (type == Particle.DustOptions.class) {
            Color color = parseColor(data.getConfigurationSection("color"), Color.WHITE);
            float size = (float) data.getDouble("size", 1.0);
            return new Particle.DustOptions(color, size);
        }
        if (type == Particle.DustTransition.class) {
            Color from = parseColor(data.getConfigurationSection("from-color"), Color.WHITE);
            Color to   = parseColor(data.getConfigurationSection("to-color"),   Color.BLACK);
            float size = (float) data.getDouble("size", 1.0);
            return new Particle.DustTransition(from, to, size);
        }
        if (type == Color.class) {
            return parseColor(data.getConfigurationSection("color"), Color.WHITE);
        }
        if (type == BlockData.class) {
            try {
                return Material.valueOf(data.getString("material", "STONE").toUpperCase()).createBlockData();
            } catch (IllegalArgumentException e) {
                return Material.STONE.createBlockData();
            }
        }
        if (type == Float.class) {
            return (float) data.getDouble("value", 0.0);
        }
        if (type == Integer.class) {
            return data.getInt("value", 0);
        }

        return null;
    }

    /**
     * Parses an RGB or ARGB color from a config section.
     * Alpha defaults to 255 (fully opaque) if not specified.
     *
     * @param section  the section containing r, g, b (and optionally a) keys
     * @param fallback color to return if the section is null
     * @return the parsed color
     */
    private static Color parseColor(@Nullable ConfigurationSection section, Color fallback) {
        if (section == null) return fallback;
        int a = section.getInt("a", 255);
        int r = section.getInt("r", 255);
        int g = section.getInt("g", 255);
        int b = section.getInt("b", 255);
        return a == 255 ? Color.fromRGB(r, g, b) : Color.fromARGB(a, r, g, b);
    }
}
