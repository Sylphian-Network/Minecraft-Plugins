package net.sylphian.minecraft.fishing.config;

import net.sylphian.minecraft.fishing.fish.LootEntry;
import net.sylphian.minecraft.fishing.fish.LootEntryType;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.services.LootService;
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
 * Loads and parses fish definitions from {@code loot_table.yml}.
 *
 * <p>Each entry in the {@code entries} section supports the following fields:</p>
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
 *
 *   <dt>{@code type}</dt>
 *   <dd>Optional. {@code ITEM} (default) builds a standard item from {@code material}.
 *       {@code CRATE_KEY} delivers a Sylphian Crates key to the player via the CratesAPI.
 *       When set to {@code CRATE_KEY}, the {@code material}, {@code display-name}, and
 *       {@code description} fields are ignored.</dd>
 *
 *   <dt>{@code key-id}</dt>
 *   <dd>Required when {@code type} is {@code CRATE_KEY}. Must match a key ID defined
 *       in Sylphian-Crates' {@code keys.yml}. Entry is skipped if absent.</dd>
 * </dl>
 *
 * @see LootEntry
 * @see LootService
 */
public class LootTableConfigLoader {

    private final FileConfiguration lootTableConfig;
    private final Logger logger;

    /**
     * Constructs a new LootTableConfigLoader.
     *
     * @param lootTableConfig the configuration containing fish definitions
     */
    public LootTableConfigLoader(FileConfiguration lootTableConfig, Logger logger) {
        this.lootTableConfig = lootTableConfig;
        this.logger = logger;
    }

    /**
     * Parses all loot table entries from the configuration.
     *
     * @return a list of parsed LootEntry objects
     * @throws IllegalArgumentException if an entry has an undefined rarity
     */
    public List<LootEntry> loadEntries() {
        List<LootEntry> entries = new ArrayList<>();
        ConfigurationSection section = lootTableConfig.getConfigurationSection("entries");

        if (section == null) {
            logger.warning("No 'entries' section found in loot_table.yml — no entries will be registered.");
            return entries;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection entrySection = section.getConfigurationSection(key);
            if (entrySection == null) continue;

            LootEntryType type;
            try {
                type = LootEntryType.valueOf(entrySection.getString("type", "ITEM").toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown type for entry '" + key + "' — defaulting to ITEM.");
                type = LootEntryType.ITEM;
            }

            String keyId = null;
            Material material = null;
            String displayName = null;
            String description = null;

            if (type == LootEntryType.CRATE_KEY) {
                keyId = entrySection.getString("key-id");
                if (keyId == null) {
                    logger.warning("Entry '" + key + "' is type CRATE_KEY but missing 'key-id' — skipping.");
                    continue;
                }
            } else {
                material    = Material.valueOf(entrySection.getString("material", "COD").toUpperCase());
                displayName = entrySection.getString("display-name", key);
                description = entrySection.getString("description", "");
            }

            String rarityId = entrySection.getString("rarity", "COMMON");
            Rarity rarity = Rarity.getById(rarityId);
            if (rarity == null) {
                logger.warning("Unknown rarity '" + rarityId + "' for entry '" + key + "' — skipping.");
                continue;
            }

            int weight       = entrySection.getInt("weight", 10);
            double minWeight = entrySection.getDouble("min-weight", 0.0);
            double maxWeight = entrySection.getDouble("max-weight", 0.0);
            List<Biome> biomes = parseBiomes(entrySection);

            Integer minY = entrySection.contains("min-y") ? entrySection.getInt("min-y") : null;
            Integer maxY = entrySection.contains("max-y") ? entrySection.getInt("max-y") : null;
            Long minTime = entrySection.contains("min-time") ? entrySection.getLong("min-time") : null;
            Long maxTime = entrySection.contains("max-time") ? entrySection.getLong("max-time") : null;

            entries.add(new LootEntry(key, type, keyId, material, displayName, description,
                    rarity, weight, biomes, minWeight, maxWeight, minY, maxY, minTime, maxTime));
        }

        logger.info("Loot table loading complete [" + entries.size() + "] entries registered.");
        return entries;
    }

    /**
     * Parses the biome restrictions for a loot entry.
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