package net.sylphian.minecraft.gathering.config;

import net.sylphian.minecraft.gathering.node.NodePlacement;
import org.bukkit.Color;
import org.bukkit.DyeColor;
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
 * @param markers               node highlight settings; never null
 * @param placements            node placements keyed by world key ({@code "sylphian:rift1"}),
 *                              each an unmodifiable list; never null
 */
public record GatheringConfig(int defaultRespawnSeconds, MarkerSettings markers,
                              Map<String, List<NodePlacement>> placements) {

    /**
     * Node highlight settings.
     *
     * @param enabled   whether markers are shown at all
     * @param scale     marker size relative to the node's block; tucked inside below 1.0
     * @param viewRange how far the glow renders, in blocks
     * @param colors    glow color by skill id; never null
     */
    public record MarkerSettings(boolean enabled, float scale, int viewRange,
                                 Map<String, Color> colors) {}

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
                parseMarkers(config.getConfigurationSection("markers"), logger),
                parsePlacements(config.getConfigurationSection("placements"), logger));
    }

    /**
     * Parses the {@code markers} section with safe defaults; a missing section yields
     * enabled defaults. Out-of-range numbers are clamped and logged rather than aborting.
     */
    private static MarkerSettings parseMarkers(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return new MarkerSettings(true, 0.98f, 6, Map.of());
        }

        boolean enabled = section.getBoolean("enabled", true);

        float scale = (float) section.getDouble("scale", 0.98);
        if (scale < 0.9f || scale > 1.0f) {
            logger.warning("markers.scale must be between 0.9 and 1.0; using 0.98.");
            scale = 0.98f;
        }

        int viewRange = section.getInt("view-range", 6);
        if (viewRange < 1 || viewRange > 64) {
            logger.warning("markers.view-range must be between 1 and 64 blocks; using 6.");
            viewRange = 6;
        }

        return new MarkerSettings(enabled, scale, viewRange,
                parseColorMap(section.getConfigurationSection("colors"), logger));
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
                logger.warning("Color '" + raw + "' at markers." + section.getName()
                        + "." + entry.getKey() + " is not a known color; skipping.");
                continue;
            }
            colors.put(entry.getKey(), color);
        }
        return Collections.unmodifiableMap(colors);
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
