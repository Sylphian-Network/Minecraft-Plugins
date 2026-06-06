package net.sylphian.minecraft.cooking.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Parses the {@code fuels} section of {@code config.yml} into a map of
 * {@link Material} → burn time in ticks.
 */
public class FuelConfigLoader {

    private final FileConfiguration config;
    private final Logger logger;

    /**
     * Constructs a new FuelConfigLoader.
     *
     * @param config the loaded {@code config.yml}
     * @param logger the plugin logger for warnings
     */
    public FuelConfigLoader(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Parses and returns all fuel entries from the config.
     *
     * @return unmodifiable map of material to burn time in ticks
     */
    public Map<Material, Integer> loadFuels() {
        Map<Material, Integer> fuels = new EnumMap<>(Material.class);
        ConfigurationSection section = config.getConfigurationSection("fuels");

        if (section == null) {
            logger.warning("No 'fuels' section found in config.yml, no fuels will be recognised.");
            return Collections.unmodifiableMap(fuels);
        }

        for (String key : section.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                int burnTime = section.getInt(key, 0);
                if (burnTime <= 0) {
                    logger.warning("Fuel '" + key + "' has non-positive burn time, skipping.");
                    continue;
                }
                fuels.put(material, burnTime);
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown fuel material '" + key + "' in config.yml, skipping.");
            }
        }

        logger.info("Fuels loaded [" + fuels.size() + "] fuel types registered.");
        return Collections.unmodifiableMap(fuels);
    }
}
