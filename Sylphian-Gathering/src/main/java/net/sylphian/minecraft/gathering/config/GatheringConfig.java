package net.sylphian.minecraft.gathering.config;

import net.sylphian.minecraft.gathering.node.NodePlacement;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Immutable snapshot of the engine's global knobs and node placements. Node
 * types come from content plugins, so nothing per-type lives here. Parse via
 * {@link #from(FileConfiguration, Logger)}; swap the reference on reload.
 *
 * @param defaultRespawnSeconds fallback respawn delay for a node type that sets none
 * @param effects               node feedback settings; never null
 * @param placements            node placements keyed by world key ({@code "sylphian:rift1"}),
 *                              each an unmodifiable list; never null
 */
public record GatheringConfig(int defaultRespawnSeconds, EffectSettings effects,
                              Map<String, List<NodePlacement>> placements) {

    /**
     * Node feedback settings: the glow marker, its per-skill colours, and the
     * per-skill harvest and replenish sounds.
     *
     * @param enabled   whether glow markers are shown at all
     * @param scale     marker size relative to the node's block; tucked inside below 1.0
     * @param viewRange how far the glow renders, in blocks
     * @param colors    glow color by skill id; never null
     * @param sounds    harvest and replenish sounds by skill id; never null
     */
    public record EffectSettings(boolean enabled, float scale, int viewRange,
                                 Map<String, Color> colors, Map<String, SoundSet> sounds) {

        /**
         * A skill's node sounds; either may be null when unset.
         *
         * @param harvest   sound played when a node is harvested, or null
         * @param replenish sound played when a node replenishes, or null
         */
        public record SoundSet(@Nullable Sound harvest, @Nullable Sound replenish) {}

        /**
         * @param skillId the node's skill id, or null
         * @return the harvest sound for the skill, or null if unset
         */
        public @Nullable Sound harvestSound(@Nullable String skillId) {
            SoundSet set = skillId == null ? null : sounds.get(skillId);
            return set == null ? null : set.harvest();
        }

        /**
         * @param skillId the node's skill id, or null
         * @return the replenish sound for the skill, or null if unset
         */
        public @Nullable Sound replenishSound(@Nullable String skillId) {
            SoundSet set = skillId == null ? null : sounds.get(skillId);
            return set == null ? null : set.replenish();
        }
    }

    /**
     * Parses the config with safe defaults; a missing or malformed key never aborts startup.
     * Placement keys without a colon and rows that are not three integers are skipped and logged.
     *
     * @param config the loaded file configuration
     * @param logger the logger for warnings
     * @return the parsed snapshot
     */
    public static GatheringConfig from(FileConfiguration config, Logger logger) {
        int defaultRespawn = config.getInt("default-respawn-seconds", 60);
        if (defaultRespawn < 1) {
            logger.warning("default-respawn-seconds must be at least 1; using 60.");
            defaultRespawn = 60;
        }

        return new GatheringConfig(defaultRespawn,
                parseEffects(config.getConfigurationSection("effects"), logger),
                parsePlacements(config.getConfigurationSection("placements"), logger));
    }

    /**
     * Parses the {@code effects} section with safe defaults; a missing section yields
     * enabled defaults. Out-of-range numbers are clamped and logged rather than aborting.
     */
    private static EffectSettings parseEffects(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return new EffectSettings(true, 0.98f, 6, Map.of(), Map.of());
        }

        boolean enabled = section.getBoolean("enabled", true);

        float scale = (float) section.getDouble("scale", 0.98);
        if (scale < 0.9f || scale > 1.0f) {
            logger.warning("effects.scale must be between 0.9 and 1.0; using 0.98.");
            scale = 0.98f;
        }

        int viewRange = section.getInt("view-range", 6);
        if (viewRange < 1 || viewRange > 64) {
            logger.warning("effects.view-range must be between 1 and 64 blocks; using 6.");
            viewRange = 6;
        }

        return new EffectSettings(enabled, scale, viewRange,
                parseColorMap(section.getConfigurationSection("colors"), logger),
                parseSounds(section.getConfigurationSection("sounds"), logger));
    }

    /**
     * Parses a color map section, skipping entries whose value is not a known color.
     *
     * @param section the config section, or null
     * @param logger  the logger for warnings
     * @return the parsed colors by key; never null
     */
    private static Map<String, Color> parseColorMap(@Nullable ConfigurationSection section, Logger logger) {
        if (section == null) return Map.of();

        Map<String, Color> colors = new HashMap<>();
        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            if (!(entry.getValue() instanceof String raw)) continue;
            Color color = parseColor(raw);
            if (color == null) {
                logger.warning("Color '" + raw + "' at effects." + section.getName()
                        + "." + entry.getKey() + " is not a known color; skipping.");
                continue;
            }
            colors.put(entry.getKey(), color);
        }
        return Collections.unmodifiableMap(colors);
    }

    /**
     * Parses the {@code sounds} section: each key is a skill id mapping to a
     * {@code harvest}/{@code replenish} pair of namespaced sound ids. Skills with
     * neither a valid harvest nor replenish sound are omitted.
     *
     * @param section the config section, or null
     * @param logger  the logger for warnings
     * @return the parsed sound sets by skill id; never null
     */
    private static Map<String, EffectSettings.SoundSet> parseSounds(@Nullable ConfigurationSection section, Logger logger) {
        if (section == null) return Map.of();

        Map<String, EffectSettings.SoundSet> sounds = new HashMap<>();
        for (String skillId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(skillId);
            if (entry == null) {
                logger.warning("effects.sounds." + skillId + " is not a section; skipping.");
                continue;
            }
            Sound harvest = parseSound(entry.getString("harvest"), skillId + ".harvest", logger);
            Sound replenish = parseSound(entry.getString("replenish"), skillId + ".replenish", logger);
            if (harvest != null || replenish != null) {
                sounds.put(skillId, new EffectSettings.SoundSet(harvest, replenish));
            }
        }
        return Collections.unmodifiableMap(sounds);
    }

    /**
     * Resolves a namespaced sound id against the sound registry.
     *
     * @param raw    the raw config value, or null
     * @param path   the config path below {@code effects.sounds}, for warnings
     * @param logger the logger for warnings
     * @return the resolved sound, or null if unset or unrecognised
     */
    private static @Nullable Sound parseSound(@Nullable String raw, String path, Logger logger) {
        if (raw == null || raw.isBlank()) return null;
        NamespacedKey key = NamespacedKey.fromString(raw.trim().toLowerCase(Locale.ROOT));
        Sound sound = key == null ? null : Registry.SOUNDS.get(key);
        if (sound == null) {
            logger.warning("Sound '" + raw + "' at effects.sounds." + path + " is not a known sound; skipping.");
        }
        return sound;
    }

    /**
     * Parses a color given as {@code "#rrggbb"} or a {@link DyeColor} name,
     * case-insensitively with dashes treated as underscores.
     *
     * @param raw the raw config value, or null
     * @return the parsed color, or null if unrecognised
     */
    private static @Nullable Color parseColor(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();

        if (value.startsWith("#")) {
            try {
                return Color.fromRGB(Integer.parseInt(value.substring(1), 16));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        try {
            return DyeColor.valueOf(value.replace('-', '_').toUpperCase(Locale.ROOT)).getColor();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Parses the {@code placements} section: each first-level key is a world key,
     * under which each {@code "namespace:id"} key maps to a list of {@code [x, y, z]}
     * rows. A missing section yields an empty map; malformed ids or rows are skipped
     * and logged.
     */
    private static Map<String, List<NodePlacement>> parsePlacements(ConfigurationSection placements, Logger logger) {
        if (placements == null) return Map.of();

        Map<String, List<NodePlacement>> byWorld = new LinkedHashMap<>();
        for (String worldKey : placements.getKeys(false)) {
            ConfigurationSection nodes = placements.getConfigurationSection(worldKey);
            if (nodes == null) {
                logger.warning("Gathering placements for world '" + worldKey + "' is not a section; skipping.");
                continue;
            }

            List<NodePlacement> parsed = new ArrayList<>();
            for (String nodeId : nodes.getKeys(false)) {
                if (nodeId.indexOf(':') == -1) {
                    logger.warning("Gathering node id '" + nodeId + "' in world '" + worldKey + "' is not 'namespace:id'; skipping.");
                    continue;
                }

                List<?> rows = nodes.getList(nodeId);
                if (rows == null) {
                    logger.warning("Gathering node '" + nodeId + "' in world '" + worldKey + "' has no coordinate list; skipping.");
                    continue;
                }

                for (Object row : rows) {
                    if (!(row instanceof List<?> coords) || coords.size() != 3
                            || !(coords.get(0) instanceof Number x)
                            || !(coords.get(1) instanceof Number y)
                            || !(coords.get(2) instanceof Number z)) {
                        logger.warning("Gathering node '" + nodeId + "' in world '" + worldKey + "' has an invalid [x, y, z] row; skipping.");
                        continue;
                    }
                    parsed.add(new NodePlacement(nodeId, x.intValue(), y.intValue(), z.intValue()));
                }
            }

            if (!parsed.isEmpty()) byWorld.put(worldKey, Collections.unmodifiableList(parsed));
        }
        return Collections.unmodifiableMap(byWorld);
    }
}
