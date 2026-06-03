package net.sylphian.minecraft.crates.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Immutable definition of a physical crate key item.
 *
 * <p>Keys are inventory items tagged with NBT. When placed in the crates GUI,
 * the plugin reads the key's ID to determine which crate it should open.</p>
 *
 * @param id          unique identifier matching the key in keys.yml
 * @param material    the item material used for the physical key item
 * @param displayName MiniMessage formatted name shown on the item
 * @param lore        MiniMessage formatted lore lines shown on the item
 * @param opens       the crate ID this key unlocks
 */
public record KeyConfig(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        String opens
) {

    /**
     * Parses a KeyConfig from a configuration section.
     *
     * @param id  the key identifier (the YAML key)
     * @param sec the configuration section for this key
     * @return the parsed KeyConfig, or null if the section is null
     */
    public static KeyConfig fromSection(String id, ConfigurationSection sec) {
        if (sec == null) return null;

        Material material;
        try {
            material = Material.valueOf(sec.getString("material", "TRIPWIRE_HOOK").toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.TRIPWIRE_HOOK;
        }

        return new KeyConfig(
                id,
                material,
                sec.getString("display-name", "<white>" + id),
                sec.getStringList("lore"),
                sec.getString("opens", "")
        );
    }
}