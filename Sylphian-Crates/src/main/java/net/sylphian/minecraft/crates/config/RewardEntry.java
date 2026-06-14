package net.sylphian.minecraft.crates.config;

import net.sylphian.minecraft.crates.economy.CrateEconomy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * @param externalItemId  namespaced item ID resolved via the ItemRegistry
 *                        (e.g. {@code "sylphian-fishing:bait/ocean_bait"});
 *                        null for standard ITEM rewards
 * @param type            the reward type — determines how it is granted
 * @param chance          relative weight in the pool; higher = more frequent
 * @param displayName     MiniMessage formatted name shown in selection GUIs;
 *                        null for external item rewards
 * @param displayMaterial material used to represent this reward in GUIs;
 *                        null for external item rewards
 * @param amount          stack size of the item given to the player
 * @param lore            MiniMessage formatted lore lines shown on the item
 * @param enchantments    map of enchantment key to level applied to the item
 * @param money           monetary amount deposited for {@link RewardType#MONEY} rewards;
 *                        zero for all other types
 */
public record RewardEntry(
        String id,
        @Nullable String externalItemId,
        RewardType type,
        double chance,
        @Nullable String displayName,
        @Nullable Material displayMaterial,
        int amount,
        List<String> lore,
        Map<String, Integer> enchantments,
        BigDecimal money
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

        String externalItemId = sec.getString("item", null);

        String displayName = null;
        Material displayMaterial = null;
        BigDecimal money = BigDecimal.ZERO;

        if (type == RewardType.MONEY) {
            money = BigDecimal.valueOf(sec.getDouble("money", 0.0)).setScale(2, RoundingMode.HALF_UP);
            try {
                displayMaterial = Material.valueOf(sec.getString("display-material", "GOLD_NUGGET").toUpperCase());
            } catch (IllegalArgumentException e) {
                displayMaterial = Material.GOLD_NUGGET;
            }
            String symbol = Bukkit.getPluginManager().getPlugin("Sylphian-Economy") != null ? CrateEconomy.currencySymbol() : "$";
            displayName = sec.getString("display-name", "<gold>" + symbol + money.toPlainString());
        } else if (externalItemId == null) {
            try {
                displayMaterial = Material.valueOf(sec.getString("display-material", "PAPER").toUpperCase());
            } catch (IllegalArgumentException e) {
                displayMaterial = Material.PAPER;
            }
            displayName = sec.getString("display-name", "<white>" + id);
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
                externalItemId,
                type,
                sec.getDouble("chance", 1.0),
                displayName,
                displayMaterial,
                sec.getInt("amount", 1),
                sec.getStringList("lore"),
                enchantments,
                money
        );
    }
}