package net.sylphian.minecraft.crates.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads and parses key definitions from {@code keys.yml}.
 */
public class KeyConfigLoader {

    private final FileConfiguration config;
    private final Logger logger;

    /**
     * Constructs a new KeyConfigLoader.
     *
     * @param config the keys.yml configuration
     * @param logger the logger for warnings
     */
    public KeyConfigLoader(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Parses all key definitions from the configuration.
     *
     * @return a map of key ID to KeyConfig
     */
    public Map<String, KeyConfig> loadKeys() {
        Map<String, KeyConfig> keys = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("keys");

        if (section == null) {
            logger.warning("No 'keys' section found in keys.yml — no keys will be registered.");
            return keys;
        }

        for (String id : section.getKeys(false)) {
            KeyConfig key = KeyConfig.fromSection(id, section.getConfigurationSection(id));
            if (key != null) {
                keys.put(id, key);
            } else {
                logger.warning("Failed to parse key '" + id + "' — skipping.");
            }
        }

        logger.info("Key loading complete [" + keys.size() + "] key(s) registered.");
        return keys;
    }
}