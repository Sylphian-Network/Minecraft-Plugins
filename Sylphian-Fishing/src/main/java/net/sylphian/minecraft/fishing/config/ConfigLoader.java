package net.sylphian.minecraft.fishing.config;

import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.weather.WeatherCondition;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads and manages plugin configuration.
 * Handles rarity definitions, mutation settings, and weather-based catch modifiers.
 */
public class ConfigLoader {

    private final Map<String, Double> mutationChances = new HashMap<>();
    private final Map<String, Boolean> mutationEnabled = new HashMap<>();
    private final Map<String, List<PotionEffect>> mutationEffects = new HashMap<>();
    private final Map<WeatherCondition, Map<String, Double>> weatherModifiers = new HashMap<>();

    private final Logger logger;

    /**
     * Constructs a new ConfigLoader and loads all configuration sections.
     *
     * @param config the file configuration to load from
     */
    public ConfigLoader(FileConfiguration config, Logger logger) {
        this.logger = logger;
        loadRarities(config.getConfigurationSection("rarities"));
        loadMutations(config.getConfigurationSection("mutations"));
        loadWeatherModifiers(config.getConfigurationSection("weather-modifiers"));
    }

    /**
     * Loads rarity definitions from the config.
     *
     * @param section the configuration section containing rarities
     */
    private void loadRarities(ConfigurationSection section) {
        Rarity.clear();
        if (section == null) {
            logger.warning("No 'rarities' section found in config.yml — no rarities will be registered.");
            return;
        }

        for (String key : section.getKeys(false)) {
            double chance = section.getDouble(key + ".chance", 1.0);
            String color = section.getString(key + ".color", "<white>");
            double mutationMultiplier = section.getDouble(key + ".mutation-multiplier", 1.0);
            Rarity.register(new Rarity(key, chance, color, mutationMultiplier));
        }

        logger.info("Rarity loading complete [" + Rarity.values().size() + "] rarities registered.");
    }

    /**
     * Loads mutation settings from the config.
     *
     * @param section the configuration section containing mutations
     */
    private void loadMutations(ConfigurationSection section) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection mutSection = section.getConfigurationSection(key);
            if (mutSection == null) continue;
            mutationEnabled.put(key, mutSection.getBoolean("enabled", false));
            mutationChances.put(key, mutSection.getDouble("base-chance", 0.0));
            mutationEffects.put(key, loadEffects(mutSection.getList("effects")));
        }
    }

    /**
     * Loads weather multipliers for each rarity.
     *
     * @param section the configuration section containing weather modifiers
     */
    private void loadWeatherModifiers(ConfigurationSection section) {
        if (section == null) return;

        for (WeatherCondition weather : WeatherCondition.values()) {
            ConfigurationSection weatherSection = section.getConfigurationSection(weather.name());
            if (weatherSection == null) continue;

            Map<String, Double> rarityMultipliers = new HashMap<>();
            for (String rarityKey : weatherSection.getKeys(false)) {
                rarityMultipliers.put(rarityKey, weatherSection.getDouble(rarityKey, 1.0));
            }

            weatherModifiers.put(weather, rarityMultipliers);
        }
    }

    /**
     * Parses a list of potion effects from the config.
     *
     * @param list the raw list from the configuration
     * @return a list of parsed PotionEffect objects
     */
    private List<PotionEffect> loadEffects(List<?> list) {
        List<PotionEffect> effects = new ArrayList<>();
        if (list == null) return effects;

        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> map)) continue;
            
            String typeName = (String) map.get("effect");
            Object durationObj = map.get("duration");
            int duration = (durationObj instanceof Number n) ? n.intValue() : 200;
            Object amplifierObj = map.get("amplifier");
            int amplifier = (amplifierObj instanceof Number n) ? n.intValue() : 0;

            if (typeName == null) continue;
            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(typeName.toLowerCase()));
            if (type != null) {
                effects.add(new PotionEffect(type, duration, amplifier));
            }
        }
        return effects;
    }

    /**
     * Retrieves the weather-based multiplier for a specific rarity.
     *
     * @param weather the current weather condition
     * @param rarity  the fish rarity
     * @return the multiplier to apply to the rarity chance
     */
    public double getWeatherMultiplier(WeatherCondition weather, Rarity rarity) {
        return weatherModifiers
                .getOrDefault(weather, Map.of())
                .getOrDefault(rarity.getId(), 1.0);
    }

    /**
     * Checks if a specific mutation is enabled.
     *
     * @param id the mutation ID
     * @return true if enabled, false otherwise
     */
    public boolean isMutationEnabled(String id) {
        return mutationEnabled.getOrDefault(id, false);
    }

    /**
     * Gets the base chance for a mutation to occur.
     *
     * @param id the mutation ID
     * @return the base chance (0.0 to 1.0)
     */
    public double getMutationBaseChance(String id) {
        return mutationChances.getOrDefault(id, 0.0);
    }

    /**
     * Gets the list of potion effects associated with a mutation.
     *
     * @param id the mutation ID
     * @return the list of potion effects
     */
    public List<PotionEffect> getMutationEffects(String id) {
        return mutationEffects.getOrDefault(id, List.of());
    }
}
