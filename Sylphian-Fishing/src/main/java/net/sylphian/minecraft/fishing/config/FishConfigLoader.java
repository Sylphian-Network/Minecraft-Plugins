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

/**
 * Loads fish definitions from a configuration file.
 * Responsible for parsing material, weight, rarity, and biome restrictions for each fish.
 */
public class FishConfigLoader {

    private final FileConfiguration fishConfig;

    /**
     * Constructs a new FishConfigLoader.
     *
     * @param fishConfig the configuration containing fish definitions
     */
    public FishConfigLoader(FileConfiguration fishConfig) {
        this.fishConfig = fishConfig;
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

        if (fishSection == null) return entries;

        for (String key : fishSection.getKeys(false)) {
            ConfigurationSection section = fishSection.getConfigurationSection(key);
            if (section == null) continue;

            Material material   = Material.valueOf(section.getString("material", "COD"));
            String displayName  = section.getString("display-name", key);
            String description  = section.getString("description", "");
            String rarityId = section.getString("rarity", "COMMON");
            Rarity rarity = Rarity.getById(rarityId);
            if (rarity == null) {
                throw new IllegalArgumentException("Unknown rarity '" + rarityId + "' for fish '" + key + "'. Please define it in config.yml");
            }
            int weight          = section.getInt("weight", 10);
            double minWeight    = section.getDouble("min-weight", 0.5);
            double maxWeight    = section.getDouble("max-weight", 3.0);
            List<Biome> biomes  = parseBiomes(section);

            entries.add(new FishEntry(key, material, displayName, description,
                    rarity, weight, biomes, minWeight, maxWeight));
        }

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