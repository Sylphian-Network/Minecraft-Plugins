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
 * Loads and parses fish definitions from {@code fish.yml}.
 *
 * <p>Each entry in the {@code fish} section supports the following fields:</p>
 *
 * <dl>
 *   <dt>{@code material} <i>(required)</i></dt>
 *   <dd>Bukkit Material name for the dropped item.</dd>
 *
 *   <dt>{@code display-name} <i>(required)</i></dt>
 *   <dd>MiniMessage formatted name shown to the player.</dd>
 *
 *   <dt>{@code rarity} <i>(required)</i></dt>
 *   <dd>Must match a rarity key defined in {@code config.yml}. Unrecognised
 *       values are logged and skipped.</dd>
 *
 *   <dt>{@code weight} <i>(required)</i></dt>
 *   <dd>Relative chance within the rarity pool. Higher values appear more often.</dd>
 *
 *   <dt>{@code description}</dt>
 *   <dd>MiniMessage lore text appended below the display name. Supports {@code \n} for line breaks.</dd>
 *
 *   <dt>{@code min-weight} / {@code max-weight}</dt>
 *   <dd>Physical catch weight range in kg. Defaults to {@code 0.5}–{@code 3.0}.</dd>
 *
 *   <dt>{@code biomes}</dt>
 *   <dd>List of Minecraft biome IDs, or the string {@code ALL} for no restriction.</dd>
 *
 *   <dt>{@code min-y} / {@code max-y}</dt>
 *   <dd>Optional Y coordinate range for the fishing hook. Omit for no restriction.</dd>
 *
 *   <dt>{@code min-time} / {@code max-time}</dt>
 *   <dd>Optional world time range in ticks (0–24000). Supports overnight ranges
 *       where {@code min-time} is greater than {@code max-time}.</dd>
 * </dl>
 *
 * @see net.sylphian.minecraft.fishing.fish.FishEntry
 * @see net.sylphian.minecraft.fishing.loot.LootManager
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

            // Y and time restrictions are optional — absence means no restriction
            Integer minY = section.contains("min-y") ? section.getInt("min-y") : null;
            Integer maxY = section.contains("max-y") ? section.getInt("max-y") : null;
            Long minTime = section.contains("min-time") ? section.getLong("min-time") : null;
            Long maxTime = section.contains("max-time") ? section.getLong("max-time") : null;

            entries.add(new FishEntry(key, material, displayName, description,
                    rarity, weight, biomes, minWeight, maxWeight, minY, maxY, minTime, maxTime));
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