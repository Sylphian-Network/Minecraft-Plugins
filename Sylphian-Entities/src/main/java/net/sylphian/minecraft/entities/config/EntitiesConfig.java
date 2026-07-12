package net.sylphian.minecraft.entities.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.entities.config.EntityProperties.PotionSpec;
import net.sylphian.minecraft.entities.item.ItemsBridge;
import net.sylphian.minecraft.entities.util.EquipmentEntry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Immutable holder for all configured entity definitions. Rebuilt on reload and
 * swapped by reference. Parsing is lenient: an invalid entity, property, or
 * enum value is skipped and logged rather than aborting the load.
 *
 * @param entities all defined entities, keyed by ID
 */
public record EntitiesConfig(Map<String, EntityDefinition> entities) {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static final Map<String, EquipmentSlot> SLOTS = Map.of(
            "head", EquipmentSlot.HEAD,
            "chest", EquipmentSlot.CHEST,
            "legs", EquipmentSlot.LEGS,
            "feet", EquipmentSlot.FEET,
            "hand", EquipmentSlot.HAND,
            "off-hand", EquipmentSlot.OFF_HAND,
            "offhand", EquipmentSlot.OFF_HAND);

    /**
     * Parses the {@code entities} section, skipping and logging invalid entries.
     *
     * @param config the file configuration to read
     * @param logger the logger for warnings
     * @return the parsed, immutable config holder
     */
    public static EntitiesConfig from(FileConfiguration config, Logger logger) {
        ConfigurationSection section = config.getConfigurationSection("entities");
        if (section == null) {
            logger.warning("No 'entities' section found in config.yml; no entities will be registered.");
            return new EntitiesConfig(Map.of());
        }

        Map<String, EntityDefinition> definitions = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection entity = section.getConfigurationSection(id);
            if (entity == null) {
                logger.warning("Entity '" + id + "' is not a section; skipping.");
                continue;
            }
            if (!id.matches("[a-z0-9._-]+")) {
                logger.warning("Entity '" + id + "' is not a valid ID (allowed: a-z, 0-9, '.', '_', '-'); skipping.");
                continue;
            }

            EntityType type = parseType(entity.getString("type"), id, logger);
            if (type == null) continue;

            Component name = entity.isString("name") ? MINI.deserialize(entity.getString("name", "")) : null;

            definitions.put(id, new EntityDefinition(
                    id,
                    type,
                    name,
                    nullableDouble(entity, "health"),
                    nullableDouble(entity, "damage"),
                    nullableDouble(entity, "armor"),
                    parseEquipment(entity.getConfigurationSection("equipment"), id, logger),
                    parseProperties(entity.getConfigurationSection("properties"), id, logger)));
        }

        return new EntitiesConfig(Collections.unmodifiableMap(definitions));
    }

    private static @Nullable EntityType parseType(@Nullable String raw, String id, Logger logger) {
        if (raw == null) {
            logger.warning("Entity '" + id + "' has no 'type'; skipping.");
            return null;
        }
        try {
            return EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("Entity '" + id + "' has unknown type '" + raw + "'; skipping.");
            return null;
        }
    }

    private static @Nullable Double nullableDouble(ConfigurationSection section, String key) {
        return section.isSet(key) ? section.getDouble(key) : null;
    }

    /**
     * Parses the {@code equipment} section. A slot maps either to a plain item
     * reference (a vanilla {@code Material} or a {@code namespace:id} custom item
     * resolved through Sylphian-Items) or to a section with {@code item} and an
     * optional {@code drop-chance} (0.0-1.0). Invalid entries are skipped and logged.
     */
    private static Map<EquipmentSlot, EquipmentEntry> parseEquipment(
            @Nullable ConfigurationSection section, String id, Logger logger) {
        if (section == null) return Map.of();

        Map<EquipmentSlot, EquipmentEntry> equipment = new EnumMap<>(EquipmentSlot.class);
        for (String slotName : section.getKeys(false)) {
            EquipmentSlot slot = SLOTS.get(slotName.toLowerCase(Locale.ROOT));
            if (slot == null) {
                logger.warning("Entity '" + id + "' has unknown equipment slot '" + slotName + "'; skipping.");
                continue;
            }

            String itemRef;
            Float dropChance = null;
            ConfigurationSection slotSection = section.getConfigurationSection(slotName);
            if (slotSection != null) {
                itemRef = slotSection.getString("item");
                if (slotSection.isSet("drop-chance")) {
                    dropChance = (float) Math.clamp(slotSection.getDouble("drop-chance"), 0.0, 1.0);
                }
            } else {
                itemRef = section.getString(slotName);
            }

            ItemStack item = resolveItem(itemRef, id, slotName, logger);
            if (item == null) continue;
            equipment.put(slot, new EquipmentEntry(item, dropChance));
        }
        return equipment;
    }

    /**
     * Resolves an item reference to an ItemStack: a vanilla {@code Material} if it
     * matches one, otherwise a {@code namespace:id} custom item via Sylphian-Items.
     * Returns null (skip-and-log) if neither resolves.
     */
    private static @Nullable ItemStack resolveItem(@Nullable String ref, String id, String slotName, Logger logger) {
        if (ref == null || ref.isBlank()) {
            logger.warning("Entity '" + id + "' has no item for slot '" + slotName + "'; skipping.");
            return null;
        }
        ref = ref.trim();

        Material material = Material.matchMaterial(ref);
        if (material != null && !material.isAir()) return new ItemStack(material);

        Optional<ItemStack> custom = ItemsBridge.resolve(ref);
        if (custom.isPresent()) return custom.get();

        logger.warning("Entity '" + id + "' has unresolved item '" + ref + "' for slot '" + slotName + "'; skipping.");
        return null;
    }

    private static EntityProperties parseProperties(
            @Nullable ConfigurationSection section, String id, Logger logger) {
        EntityProperties defaults = EntityProperties.DEFAULTS;
        if (section == null) return defaults;

        return new EntityProperties(
                section.getBoolean("glowing", defaults.glowing()),
                section.getBoolean("baby", defaults.baby()),
                section.getBoolean("name-visible", defaults.nameVisible()),
                section.getBoolean("persistent", defaults.persistent()),
                parsePotionEffects(section.getStringList("potion-effects"), id, logger));
    }

    /**
     * Parses {@code "type:amplifier:duration"} entries, where duration is a tick
     * count or {@code "infinite"}. Amplifier and duration are optional and
     * default to 0 and infinite. Invalid entries are skipped and logged.
     */
    private static List<PotionSpec> parsePotionEffects(List<String> raw, String id, Logger logger) {
        List<PotionSpec> effects = new ArrayList<>();
        for (String entry : raw) {
            String[] parts = entry.split(":");
            String name = parts[0].trim().toLowerCase(Locale.ROOT);
            // NamespacedKey construction throws on an empty or invalid key; guard
            // so a typo skips-and-logs instead of aborting the whole config load.
            PotionEffectType type = null;
            if (!name.isEmpty()) {
                try {
                    type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(name));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (type == null) {
                logger.warning("Entity '" + id + "' has unknown potion effect '" + parts[0] + "'; skipping.");
                continue;
            }

            int amplifier = parts.length > 1 ? parseInt(parts[1].trim(), 0) : 0;
            int duration = parts.length > 2 ? parseDuration(parts[2].trim()) : PotionEffect.INFINITE_DURATION;
            effects.add(new PotionSpec(type, amplifier, duration));
        }
        return effects;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseDuration(String value) {
        if (value.equalsIgnoreCase("infinite")) return PotionEffect.INFINITE_DURATION;
        return parseInt(value, PotionEffect.INFINITE_DURATION);
    }
}
