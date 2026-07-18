package net.sylphian.minecraft.logging.item;

import net.sylphian.minecraft.items.item.ItemProvider;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Exposes Sylphian-Logging's wood items to the cross-plugin item registry.
 */
public final class LoggingItemProvider implements ItemProvider {

    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("sylphian-logging", "item_id");

    private record ItemDefinition(Material material, String name, List<String> lore) {}

    private final Logger logger;
    private volatile Map<String, ItemDefinition> items;

    public LoggingItemProvider(ConfigurationSection itemsSection, Logger logger) {
        this.logger = logger;
        this.items = parse(itemsSection);
    }

    @Override
    public String namespace() {
        return "sylphian-logging";
    }

    @Override
    public Optional<ItemStack> provide(String itemId) {
        Map<String, ItemDefinition> current = items;
        ItemDefinition def = current.get(itemId);
        if (def == null) return Optional.empty();

        ItemStack item = new ItemBuilder(def.material())
                .name(def.name())
                .loreStrings(def.lore())
                .build();
        item.editMeta(meta -> meta.getPersistentDataContainer()
                .set(ITEM_ID_KEY, PersistentDataType.STRING, itemId));
        return Optional.of(item);
    }

    @Override
    public Set<String> itemIds() {
        return Set.copyOf(items.keySet());
    }

    /**
     * Rebuilds the item definitions after a config reload.
     *
     * @param itemsSection the reloaded {@code items} section, may be null
     */
    public void reload(ConfigurationSection itemsSection) {
        this.items = parse(itemsSection);
    }

    private Map<String, ItemDefinition> parse(ConfigurationSection section) {
        Map<String, ItemDefinition> parsed = new LinkedHashMap<>();
        if (section == null) {
            logger.warning("No 'items' section in config.yml; no logging items will be registered.");
            return parsed;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(id);
            if (item == null) continue;

            String materialName = item.getString("material", "");
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                logger.warning("Item '" + id + "' has unknown material '" + materialName + "'; skipping.");
                continue;
            }

            String name = item.getString("name", id);
            List<String> lore = item.getStringList("lore");
            parsed.put(id, new ItemDefinition(material, name, lore));
        }
        return parsed;
    }
}
