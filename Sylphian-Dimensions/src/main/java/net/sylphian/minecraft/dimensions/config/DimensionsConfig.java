package net.sylphian.minecraft.dimensions.config;

import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.model.DimensionRuleset;
import net.sylphian.minecraft.dimensions.spawn.SpawnEntry;
import net.sylphian.minecraft.dimensions.spawn.SpawnSettings;
import net.sylphian.minecraft.dimensions.spawn.TimeWindow;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Immutable holder for all Sylphian-Dimensions configuration.
 * Rebuilt on reload and swapped by reference.
 *
 * @param hubName            the dimension used as the lobby and redirect target
 * @param spawnIntervalTicks the tick interval between custom-spawner cycles
 * @param dimensions         all defined dimensions, keyed by name
 */
public record DimensionsConfig(String hubName, int spawnIntervalTicks, Map<String, Dimension> dimensions) {

    /**
     * Parses the configuration, skipping and logging invalid entries.
     * A bad dimension entry never aborts the load.
     *
     * @param config the file configuration to read
     * @param logger the logger for warnings
     * @return the parsed, immutable config holder
     */
    public static DimensionsConfig from(FileConfiguration config, Logger logger) {
        String hubName = config.getString("hub-dimension", "hub");
        int spawnIntervalTicks = Math.max(1, config.getInt("spawning.interval-ticks", 20));

        ConfigurationSection section = config.getConfigurationSection("dimensions");
        if (section == null) {
            logger.warning("No 'dimensions' section found in config.yml; no dimensions will be loaded.");
            return new DimensionsConfig(hubName, spawnIntervalTicks, Map.of());
        }

        Map<String, Dimension> dimensions = new LinkedHashMap<>();
        for (String name : section.getKeys(false)) {
            ConfigurationSection dim = section.getConfigurationSection(name);
            if (dim == null) {
                logger.warning("Dimension '" + name + "' is not a section; skipping.");
                continue;
            }

            // Dimension names become world keys (sylphian:<name>), so they must be valid key characters
            if (!name.matches("[a-z0-9._-]+")) {
                logger.warning("Dimension '" + name + "' is not a valid world key (allowed: a-z, 0-9, '.', '_', '-'); skipping.");
                continue;
            }

            dimensions.put(name, new Dimension(
                    name,
                    dim.getString("template", name),
                    dim.getInt("template-version", 1),
                    parseSpawnPoint(dim.getDoubleList("spawn-point")),
                    boundsAt(dim.getIntegerList("chunk-bounds"), 0),
                    boundsAt(dim.getIntegerList("chunk-bounds"), 1),
                    parseRuleset(dim),
                    parseSpawns(dim.getConfigurationSection("spawns"), name, logger)));
        }

        if (!dimensions.containsKey(hubName)) {
            logger.severe("Hub dimension '" + hubName + "' is not defined under 'dimensions'.");
        }

        return new DimensionsConfig(hubName, spawnIntervalTicks, Collections.unmodifiableMap(dimensions));
    }

    /**
     * Parses a spawn point from a list of 3 (x, y, z) or 5 (x, y, z, yaw, pitch) values.
     * Falls back to 0.5, 65, 0.5 facing south.
     */
    private static Dimension.SpawnPoint parseSpawnPoint(List<Double> values) {
        if (values.size() < 3) return new Dimension.SpawnPoint(0.5, 65.0, 0.5, 0f, 0f);
        float yaw = values.size() >= 4 ? values.get(3).floatValue() : 0f;
        float pitch = values.size() >= 5 ? values.get(4).floatValue() : 0f;
        return new Dimension.SpawnPoint(values.get(0), values.get(1), values.get(2), yaw, pitch);
    }

    private static int boundsAt(List<Integer> values, int index) {
        return values.size() > index ? values.get(index) : 10;
    }

    /**
     * Parses the {@code dimension-rules} section of a dimension; a missing
     * section or key falls back to {@link DimensionRuleset#DEFAULTS}.
     */
    private static DimensionRuleset parseRuleset(ConfigurationSection dim) {
        DimensionRuleset defaults = DimensionRuleset.DEFAULTS;
        ConfigurationSection rules = dim.getConfigurationSection("dimension-rules");
        if (rules == null) return defaults;
        return new DimensionRuleset(
                rules.getBoolean("pvp", defaults.pvpEnabled()),
                rules.getBoolean("building", defaults.buildingEnabled()),
                rules.getBoolean("natural-spawning", defaults.naturalSpawning()),
                rules.getBoolean("natural-growth", defaults.naturalGrowth()),
                rules.getBoolean("claiming", defaults.claimingEnabled()),
                rules.getBoolean("damage-enabled", defaults.damageEnabled()),
                rules.getBoolean("keep-inventory", defaults.keepInventory()),
                rules.getBoolean("login-redirect", defaults.loginRedirect()),
                rules.getDouble("death-loss-chance", defaults.deathLossChance()));
    }

    /**
     * Parses the {@code spawns} section of a dimension. A missing section
     * disables custom spawning; invalid entries are skipped and logged.
     */
    private static SpawnSettings parseSpawns(ConfigurationSection spawns, String dimensionName, Logger logger) {
        if (spawns == null) return SpawnSettings.DISABLED;

        List<SpawnEntry> entries = new ArrayList<>();
        List<Map<?, ?>> rawEntries = spawns.getMapList("entries");
        for (Map<?, ?> raw : rawEntries) {
            SpawnEntry entry = parseSpawnEntry(raw, dimensionName, logger);
            if (entry != null) entries.add(entry);
        }

        boolean enabled = spawns.getBoolean("enabled", true) && !entries.isEmpty();
        return new SpawnSettings(
                enabled,
                Math.max(0, spawns.getInt("mob-cap", 20)),
                Math.max(1, spawns.getInt("per-chunk-cap", 4)),
                Math.max(1, spawns.getInt("spawn-range-chunks", 8)),
                Collections.unmodifiableList(entries));
    }

    /**
     * Parses one spawn-table row from a map. A missing or invalid entity
     * reference or time window skips the row; numeric fields fall back to
     * sensible defaults.
     */
    private static SpawnEntry parseSpawnEntry(Map<?, ?> raw, String dimensionName, Logger logger) {
        Object entityId = raw.get("entity");
        if (!(entityId instanceof String id) || id.indexOf(':') == -1) {
            logger.warning("Dimension '" + dimensionName + "' has a spawn entry with a missing or malformed 'entity' (expected 'namespace:id'); skipping.");
            return null;
        }

        TimeWindow time;
        Object rawTime = raw.get("time");
        try {
            time = rawTime == null ? TimeWindow.ANY : TimeWindow.valueOf(rawTime.toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("Dimension '" + dimensionName + "' spawn entry '" + id + "' has unknown time '" + rawTime + "'; skipping.");
            return null;
        }

        int minGroup = Math.max(1, intOr(raw.get("min-group"), 1));
        int maxGroup = Math.max(minGroup, intOr(raw.get("max-group"), minGroup));
        return new SpawnEntry(
                id,
                Math.max(1, intOr(raw.get("weight"), 1)),
                time,
                minGroup,
                maxGroup,
                clampLight(intOr(raw.get("min-light"), 0)),
                clampLight(intOr(raw.get("max-light"), 7)),
                intOr(raw.get("min-y"), -64),
                intOr(raw.get("max-y"), 320));
    }

    private static int intOr(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static int clampLight(int value) {
        return Math.clamp(value, 0, 15);
    }
}
