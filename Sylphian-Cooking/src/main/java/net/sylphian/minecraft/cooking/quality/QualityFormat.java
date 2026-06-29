package net.sylphian.minecraft.cooking.quality;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Presentation rules for a single quality tier.
 * Applied by {@link CookingQuality#applyTo} to append a lore line to the output item.
 *
 * @param loreLine MiniMessage string appended as a new lore line on the output item
 */
public record QualityFormat(String loreLine) {

    /**
     * Parses a {@link QualityFormat} from a configuration section.
     * Falls back to {@code defaultLore} when the key is absent or the section is null.
     *
     * @param section     the config section to read, or null to use the default
     * @param defaultLore default lore line if the key is missing
     * @return the parsed format
     */
    public static QualityFormat from(ConfigurationSection section, String defaultLore) {
        if (section == null) return new QualityFormat(defaultLore);
        return new QualityFormat(section.getString("lore-line", defaultLore));
    }
}
