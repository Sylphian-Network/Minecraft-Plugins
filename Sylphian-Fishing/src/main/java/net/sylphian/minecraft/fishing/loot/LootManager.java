package net.sylphian.minecraft.fishing.loot;

import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.fish.FishEntry;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.util.ItemBuilder;
import net.sylphian.minecraft.fishing.weather.WeatherCondition;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the loot rolling logic for fishing.
 * Responsible for selecting a fish based on rarity, biome, and weather conditions.
 * Uses a weighted random selection for fish within a rarity pool.
 */
public class LootManager {

    private final Map<Rarity, List<FishEntry>> poolsByRarity;
    private final ConfigLoader config;
    private final Random random = new Random();

    /**
     * Constructs a new LootManager.
     *
     * @param allFish the list of all available fish entries
     * @param config  the configuration loader for retrieving multipliers
     */
    public LootManager(List<FishEntry> allFish, ConfigLoader config) {
        this.poolsByRarity = allFish.stream()
                .collect(Collectors.groupingBy(FishEntry::getRarity));
        this.config = config;
    }

    /**
     * Rolls a catch result for the given biome and weather condition.
     * Walks rarities from rarest to most common using a single random roll,
     * applying weather multipliers to each rarity's base chance.
     *
     * @param biome   the biome where the fishing hook landed
     * @param weather the current weather condition in the world
     * @return a CatchResult containing the fish ID, rarity, weight, and built ItemStack
     */
    public CatchResult rollCatch(Biome biome, WeatherCondition weather) {
        double roll = random.nextDouble();

        // Iterate through rarities in descending order of rarity (rarest first)
        for (Rarity rarity : Rarity.byDescendingRarity()) {
            double baseChance = rarity.getChance();
            double multiplier = config.getWeatherMultiplier(weather, rarity);
            // Multiplier can increase chance, but it's clamped to 1.0 (100%)
            double finalChance = Math.min(1.0, baseChance * multiplier);

            // If the roll is higher than the chance, this rarity was not hit
            if (roll > finalChance) continue;

            // Filter the pool of fish for this rarity by the current biome
            List<FishEntry> pool = poolsByRarity
                    .getOrDefault(rarity, List.of())
                    .stream()
                    .filter(f -> f.appliesToBiome(biome))
                    .toList();

            if (pool.isEmpty()) continue;

            return buildCatchResult(weightedPick(pool));
        }

        // Fallback logic if no rarity was rolled or no fish matched the biome
        Rarity fallback = getFallbackRarity();

        if (fallback != null) {
            List<FishEntry> pool = poolsByRarity
                    .getOrDefault(fallback, List.of())
                    .stream()
                    .filter(f -> f.appliesToBiome(biome))
                    .toList();

            if (!pool.isEmpty()) {
                return buildCatchResult(weightedPick(pool));
            }

            return new CatchResult("fallback", fallback, 0.0, new ItemStack(Material.COD));
        }

        return new CatchResult("fallback", null, 0.0, new ItemStack(Material.COD));
    }

    /**
     * Determines the fallback rarity (usually the most common one).
     *
     * @return the rarity with the highest chance (most common)
     */
    private Rarity getFallbackRarity() {
        return Rarity.values().stream()
                .min(Comparator.comparingDouble(Rarity::getChance))
                .orElse(null);
    }

    /**
     * Performs a weighted random selection from a pool of fish entries.
     *
     * @param pool the list of fish entries to pick from
     * @return the selected fish entry
     */
    private FishEntry weightedPick(List<FishEntry> pool) {
        int totalWeight = pool.stream().mapToInt(FishEntry::getWeight).sum();
        int roll = random.nextInt(totalWeight);
        int cursor = 0;

        // Iterate through the pool and accumulate weights until the roll is matched
        for (FishEntry fish : pool) {
            cursor += fish.getWeight();
            if (roll < cursor) return fish;
        }

        return pool.getFirst();
    }

    /**
     * Builds a CatchResult for a specific fish, including rolling its weight.
     *
     * @param fish the fish that was caught
     * @return the complete catch result
     */
    private CatchResult buildCatchResult(FishEntry fish) {
        double weight = fish.rollWeight(random);
        ItemStack itemStack = buildItemStack(fish, weight);
        return new CatchResult(fish.getId(), fish.getRarity(), weight, itemStack);
    }

    /**
     * Builds the ItemStack for the caught fish, setting its name and lore.
     *
     * @param fish         the fish entry
     * @param caughtWeight the rolled weight of the fish
     * @return the built ItemStack
     */
    private ItemStack buildItemStack(FishEntry fish, double caughtWeight) {
        return new ItemBuilder(fish.getMaterial())
                .name(fish.getDisplayName())
                .loreStrings(buildLore(fish, caughtWeight))
                .build();
    }

    /**
     * Builds the lore for the fish item, including description, rarity, and weight.
     *
     * @param fish         the fish entry
     * @param caughtWeight the rolled weight of the fish
     * @return a list of lore strings
     */
    private List<String> buildLore(FishEntry fish, double caughtWeight) {
        List<String> lore = new ArrayList<>();

        if (!fish.getDescription().isEmpty()) {
            lore.addAll(Arrays.asList(fish.getDescription().split("\n")));
            lore.add("");
        }

        lore.add(String.format("<gray>Rarity: %s", fish.getRarity().getDisplayName()));
        lore.add(String.format("<gray>Weight: <white>%.2fkg", caughtWeight));

        return lore;
    }
}