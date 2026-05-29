package net.sylphian.minecraft.fishing.config;

import net.sylphian.minecraft.fishing.fish.Rarity;
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

public class ConfigLoader {

    private final Map<String, Double> mutationChances = new HashMap<>();
    private final Map<String, Boolean> mutationEnabled = new HashMap<>();
    private final Map<String, List<PotionEffect>> mutationEffects = new HashMap<>();

    public ConfigLoader(FileConfiguration config) {
        loadRarities(config.getConfigurationSection("rarities"));
        loadMutations(config.getConfigurationSection("mutations"));
    }

    private void loadRarities(ConfigurationSection section) {
        Rarity.clear();
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            double chance = section.getDouble(key + ".chance", 1.0);
            String color = section.getString(key + ".color", "<white>");
            double mutationMultiplier = section.getDouble(key + ".mutation-multiplier", 1.0);
            Rarity.register(new Rarity(key, chance, color, mutationMultiplier));
        }
    }

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

    public boolean isMutationEnabled(String id) {
        return mutationEnabled.getOrDefault(id, false);
    }

    public double getMutationBaseChance(String id) {
        return mutationChances.getOrDefault(id, 0.0);
    }

    public List<PotionEffect> getMutationEffects(String id) {
        return mutationEffects.getOrDefault(id, List.of());
    }
}
