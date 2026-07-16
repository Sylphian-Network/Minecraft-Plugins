package net.sylphian.minecraft.gathering.config;

import net.sylphian.minecraft.gathering.node.NodePlacement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Immutable snapshot of the engine's global knobs and node placements. Node
 * types come from content plugins, so nothing per-type lives here. Parse via
 * {@link #from(FileConfiguration, Logger)}; swap the reference on reload.
 *
 * @param defaultRespawnSeconds fallback respawn delay for a node type that sets none
 * @param placements            node placements keyed by world key ({@code "sylphian:rift1"}),
 *                              each an unmodifiable list; never null
 */
public record GatheringConfig(int defaultRespawnSeconds, Map<String, List<NodePlacement>> placements) {

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

        return new GatheringConfig(defaultRespawn, parsePlacements(config.getConfigurationSection("placements"), logger));
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
