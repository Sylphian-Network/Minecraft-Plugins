package net.sylphian.minecraft.fishing.config;

import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads and manages plugin configuration.
 * Handles rarity definitions, mutation settings, and weather-based catch modifiers.
 */
public class ConfigLoader {

    private final Map<String, MutationConfig> mutations = new HashMap<>();
    private final Map<WeatherCondition, Map<String, Double>> weatherModifiers = new HashMap<>();
    private final Map<String, RarityCatchEffects> rarityCatchEffects = new HashMap<>();

    private BiteTimerConfig biteTimerConfig;

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
        loadBiteTimer(config.getConfigurationSection("bite-timer"));
    }

    /**
     * Loads rarity definitions and their associated catch effects from the config.
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

            ConfigurationSection effectsSection = section.getConfigurationSection(key + ".catch-effects");
            if (effectsSection == null) {
                rarityCatchEffects.put(key.toUpperCase(), RarityCatchEffects.empty());
                continue;
            }

            rarityCatchEffects.put(key.toUpperCase(), new RarityCatchEffects(
                    RarityCatchEffects.SoundConfig.fromSection(effectsSection.getConfigurationSection("sound")),
                    RarityCatchEffects.ParticleConfig.fromSection(effectsSection.getConfigurationSection("particles")),
                    RarityCatchEffects.TitleConfig.fromSection(effectsSection.getConfigurationSection("title")),
                    RarityCatchEffects.BroadcastConfig.fromSection(effectsSection.getConfigurationSection("broadcast"))
            ));
        }

        logger.info("Rarity loading complete [" + Rarity.values().size() + "] rarities registered.");
    }

    /**
     * Loads mutation settings from the config into a single MutationConfig per mutation.
     *
     * @param section the configuration section containing mutations
     */
    private void loadMutations(ConfigurationSection section) {
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            mutations.put(key, MutationConfig.fromSection(section.getConfigurationSection(key)));
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
     * Loads the bite and lure timer configuration from the {@code bite-timer} section,
     * falling back to sensible defaults if the section is missing.
     *
     * @param section the {@code bite-timer} configuration section
     */
    private void loadBiteTimer(ConfigurationSection section) {
        if (section == null) {
            biteTimerConfig = new BiteTimerConfig(100, 600, Map.of(), Map.of(), 20, 80, Map.of());
            return;
        }

        int baseMin = section.getInt("base-min", 100);
        int baseMax = section.getInt("base-max", 600);

        int lureBaseMin = 20;
        int lureBaseMax = 80;

        Map<Rarity, Double> rarityModifiers = new HashMap<>();
        ConfigurationSection raritySection = section.getConfigurationSection("rarity-modifiers");
        if (raritySection != null) {
            for (String key : raritySection.getKeys(false)) {
                Rarity rarity = Rarity.getById(key);
                if (rarity != null) {
                    rarityModifiers.put(rarity, raritySection.getDouble(key, 1.0));
                }
            }
        }

        Map<Rarity, Double> lureRarityModifiers = new HashMap<>();
        ConfigurationSection lureSection = section.getConfigurationSection("lure-timer");
        if (lureSection != null) {
            lureBaseMin = lureSection.getInt("base-min", 20);
            lureBaseMax = lureSection.getInt("base-max", 80);

            ConfigurationSection lureRaritySection = lureSection.getConfigurationSection("rarity-modifiers");
            if (lureRaritySection != null) {
                for (String key : lureRaritySection.getKeys(false)) {
                    Rarity rarity = Rarity.getById(key);
                    if (rarity != null) {
                        lureRarityModifiers.put(rarity, lureRaritySection.getDouble(key, 1.0));
                    }
                }
            }
        }

        Map<WeatherCondition, Double> weatherModifiers = new HashMap<>();
        ConfigurationSection weatherSection = section.getConfigurationSection("weather-modifiers");
        if (weatherSection != null) {
            for (WeatherCondition weather : WeatherCondition.values()) {
                weatherModifiers.put(weather,
                        weatherSection.getDouble(weather.name(), 1.0));
            }
        }

        biteTimerConfig = new BiteTimerConfig(baseMin, baseMax, rarityModifiers, weatherModifiers, lureBaseMin, lureBaseMax, lureRarityModifiers);
    }

    /**
     * Retrieves the catch effects configuration for a given rarity.
     *
     * @param rarity the rarity to get effects for
     * @return the RarityCatchEffects, or an empty config if none defined
     */
    public RarityCatchEffects getRarityCatchEffects(Rarity rarity) {
        return rarityCatchEffects.getOrDefault(rarity.getId().toUpperCase(),
                RarityCatchEffects.empty());
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
     * Returns the loaded bite and lure timer configuration.
     *
     * @return the BiteTimerConfig
     */
    public BiteTimerConfig getBiteTimerConfig() { return biteTimerConfig; }

    /**
     * Retrieves the full mutation configuration for a given mutation ID.
     *
     * @param id the mutation ID
     * @return the MutationConfig, or an empty disabled config if not found
     */
    public MutationConfig getMutationConfig(String id) {
        return mutations.getOrDefault(id, MutationConfig.empty());
    }
}
