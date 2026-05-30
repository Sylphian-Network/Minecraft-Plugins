package net.sylphian.minecraft.fishing.config;

import net.sylphian.minecraft.fishing.fish.FishEntry;
import net.sylphian.minecraft.fishing.fish.Rarity;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Loads fish definitions from a configuration file.
 * Responsible for parsing material, weight, rarity, and biome restrictions for each fish.
 */
public class FishConfigLoader {

    private final FileConfiguration fishConfig;
    private final Logger logger;

    /**
     * Constructs a new FishConfigLoader.
     *
     * @param fishConfig the configuration containing fish definitions
     */
    public FishConfigLoader(FileConfiguration fishConfig, Logger logger) {
        this.fishConfig = fishConfig;
        this.logger = logger;
    }

    /**
     * Parses all fish entries from the configuration.
     *
     * @return a list of parsed FishEntry objects
     * @throws IllegalArgumentException if a fish has an undefined rarity
     */
    public List<FishEntry> loadFish() {
        List<FishEntry> entries = new ArrayList<>();
        ConfigurationSection fishSection = fishConfig.getConfigurationSection("fish");

        if (fishSection == null) {
            logger.warning("No 'fish' section found in fish.yml — no fish will be registered.");
            return entries;
        }

        for (String key : fishSection.getKeys(false)) {
            ConfigurationSection section = fishSection.getConfigurationSection(key);
            if (section == null) continue;

            Material material   = Material.valueOf(section.getString("material", "COD"));
            String displayName  = section.getString("display-name", key);
            String description  = section.getString("description", "");
            String rarityId = section.getString("rarity", "COMMON");
            Rarity rarity = Rarity.getById(rarityId);
            if (rarity == null) {
                logger.warning("Unknown rarity '" + rarityId + "' for fish '" + key + "' - skipping.");
                continue;
            }
            int weight          = section.getInt("weight", 10);
            double minWeight    = section.getDouble("min-weight", 0.5);
            double maxWeight    = section.getDouble("max-weight", 3.0);
            List<Biome> biomes  = parseBiomes(section);
            Integer minY = section.contains("min-y") ? section.getInt("min-y") : null;
            Integer maxY = section.contains("max-y") ? section.getInt("max-y") : null;

            entries.add(new FishEntry(key, material, displayName, description,
                    rarity, weight, biomes, minWeight, maxWeight, minY, maxY));
        }

        logger.info("Fish loading complete [" + entries.size() + "] fish(s) registered.");
        return entries;
    }

    /**
     * Parses the biome restrictions for a fish.
     * If "ALL" is specified, the fish can be caught anywhere.
     *
     * @param section the configuration section for a specific fish
     * @return a list of allowed biomes, or an empty list for "ALL"
     */
    private List<Biome> parseBiomes(ConfigurationSection section) {
        if ("ALL".equals(section.getString("biomes"))) return List.of();

        var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);

        return section.getStringList("biomes").stream()
                .map(b -> biomeRegistry.get(NamespacedKey.minecraft(b.toLowerCase())))
                .filter(Objects::nonNull)
                .toList();
    }
}