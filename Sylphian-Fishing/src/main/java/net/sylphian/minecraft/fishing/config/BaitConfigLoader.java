package net.sylphian.minecraft.fishing.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads bait definitions from {@code baits.yml}.
 */
public class BaitConfigLoader {

    private final FileConfiguration config;
    private final Logger logger;

    /**
     * Constructs a new BaitConfigLoader.
     *
     * @param config the baits.yml configuration
     * @param logger the logger for warnings
     */
    public BaitConfigLoader(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Parses all bait definitions from the configuration.
     *
     * @return a map of bait ID to BaitConfig
     */
    public Map<String, BaitConfig> loadBaits() {
        Map<String, BaitConfig> baits = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("baits");

        if (section == null) {
            logger.warning("No 'baits' section found in baits.yml. no baits will be registered.");
            return baits;
        }

        for (String id : section.getKeys(false)) {
            BaitConfig bait = BaitConfig.fromSection(id, section.getConfigurationSection(id));
            if (bait != null) baits.put(id, bait);
        }

        logger.info("Bait loading complete [" + baits.size() + "] bait(s) registered.");
        return baits;
    }
}