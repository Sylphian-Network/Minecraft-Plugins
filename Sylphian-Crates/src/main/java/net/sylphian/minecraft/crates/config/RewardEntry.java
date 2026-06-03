package net.sylphian.minecraft.crates.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable definition of a single reward entry in a crate's reward pool.
 *
 * <p>Each entry has a weighted {@code chance} used during the roll. Entries
 * with higher chances appear more often relative to others in the same pool.</p>
 *
 * @param id              unique identifier within the crate's pool
 * @param type            the reward type — determines how it is granted
 * @param chance          relative weight in the pool; higher = more frequent
 * @param displayName     MiniMessage formatted name shown in selection GUIs
 * @param displayMaterial material used to represent this reward in GUIs
 * @param amount          stack size of the item given to the player
 * @param lore            MiniMessage formatted lore lines shown on the item
 * @param enchantments    map of enchantment key to level applied to the item
 */
public record RewardEntry(
        String id,
        RewardType type,
        double chance,
        String displayName,
        Material displayMaterial,
        int amount,
        List<String> lore,
        Map<String, Integer> enchantments
) {
    /**
     * Parses a RewardEntry from a configuration section.
     *
     * @param id  the reward identifier (the YAML key within the pool)
     * @param sec the configuration section for this reward
     * @return the parsed RewardEntry, or null if the section is null or the type is invalid
     */
    public static RewardEntry fromSection(String id, ConfigurationSection sec) {
        if (sec == null) return null;

        RewardType type;
        try {
            type = RewardType.valueOf(sec.getString("type", "ITEM").toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        Material displayMaterial;
        try {
            displayMaterial = Material.valueOf(sec.getString("display-material", "PAPER").toUpperCase());
        } catch (IllegalArgumentException e) {
            displayMaterial = Material.PAPER;
        }

        Map<String, Integer> enchantments = new HashMap<>();
        ConfigurationSection enchantSection = sec.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String enchKey : enchantSection.getKeys(false)) {
                enchantments.put(enchKey.toLowerCase(), enchantSection.getInt(enchKey, 1));
            }
        }

        return new RewardEntry(
                id,
                type,
                sec.getDouble("chance", 1.0),
                sec.getString("display-name", "<white>" + id),
                displayMaterial,
                sec.getInt("amount", 1),
                sec.getStringList("lore"),
                enchantments
        );
    }
}