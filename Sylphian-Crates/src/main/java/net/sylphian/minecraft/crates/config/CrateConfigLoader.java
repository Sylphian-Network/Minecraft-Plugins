package net.sylphian.minecraft.crates.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads and parses crate definitions from {@code crates.yml}.
 */
public class CrateConfigLoader {

    private final FileConfiguration config;
    private final Logger logger;

    /**
     * Constructs a new CrateConfigLoader.
     *
     * @param config the crates.yml configuration
     * @param logger the logger for warnings
     */
    public CrateConfigLoader(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Parses all crate definitions from the configuration.
     *
     * @return a map of crate ID to CrateConfig
     */
    public Map<String, CrateConfig> loadCrates() {
        Map<String, CrateConfig> crates = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("crates");

        if (section == null) {
            logger.warning("No 'crates' section found in crates.yml — no crates will be registered.");
            return crates;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection crateSection = section.getConfigurationSection(id);
            if (crateSection == null) continue;

            Material displayMaterial;
            try {
                displayMaterial = Material.valueOf(crateSection.getString("display-material", "CHEST").toUpperCase());
            } catch (IllegalArgumentException e) {
                displayMaterial = Material.CHEST;
            }

            OpeningStyle openingStyle;
            try {
                openingStyle = OpeningStyle.valueOf(
                        crateSection.getString("opening-style", "SELECTION").toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Crate '" + id + "' has an invalid opening-style — defaulting to SELECTION.");
                openingStyle = OpeningStyle.SELECTION;
            }

            List<RewardEntry> pool = parsePool(id, crateSection.getConfigurationSection("pool"));
            if (pool.isEmpty()) {
                logger.warning("Crate '" + id + "' has an empty reward pool — skipping.");
                continue;
            }

            crates.put(id, new CrateConfig(
                    id,
                    crateSection.getString("display-name", "<white>" + id),
                    displayMaterial,
                    crateSection.getInt("total-rolls", 1),
                    crateSection.getInt("player-picks", 1),
                    openingStyle,
                    pool
            ));
        }

        logger.info("Crate loading complete [" + crates.size() + "] crate(s) registered.");
        return crates;
    }

    /**
     * Parses the reward pool for a single crate.
     *
     * @param crateId     the crate ID, used for warning messages
     * @param poolSection the configuration section containing pool entries
     * @return a list of parsed RewardEntry objects
     */
    private List<RewardEntry> parsePool(String crateId, ConfigurationSection poolSection) {
        List<RewardEntry> pool = new ArrayList<>();
        if (poolSection == null) return pool;

        for (String rewardId : poolSection.getKeys(false)) {
            RewardEntry entry = RewardEntry.fromSection(rewardId, poolSection.getConfigurationSection(rewardId));
            if (entry != null) {
                pool.add(entry);
            } else {
                logger.warning("Failed to parse reward '" + rewardId + "' in crate '" + crateId + "' — skipping.");
            }
        }

        return pool;
    }
}