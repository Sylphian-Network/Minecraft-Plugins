package net.sylphian.minecraft.fishing.services;

import net.sylphian.minecraft.fishing.config.BaitConfig;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.fish.*;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the loot rolling logic for the fishing system.
 *
 * <p>Rather than rolling a rarity first and then searching for fish, the manager
 * builds an <b>eligible pool</b> upfront from all fish that match the current
 * catch context. Rarity is then rolled and applied as a filter against that pool,
 * ensuring biome, height, and time restrictions are always respected.</p>
 *
 * <h2>Roll Process</h2>
 * <ol>
 *   <li>Collect all fish matching the current biome, Y coordinate, and time of day</li>
 *   <li>Roll a rarity using base chances modified by weather multipliers</li>
 *   <li>Filter the eligible pool to fish of the rolled rarity</li>
 *   <li>Pick a fish using weighted random selection</li>
 *   <li>If no fish of the rolled rarity exist in the eligible pool, fall back to
 *       a weighted pick across the entire eligible pool respecting rarity chances</li>
 * </ol>
 *
 * <h2>Weather Modifiers</h2>
 * <p>Each weather condition ({@link WeatherCondition})
 * applies a multiplier to each rarity's base chance. Multipliers are loaded from
 * {@code config.yml} and clamped to a maximum of {@code 1.0}.</p>
 *
 * @see LootEntry
 * @see net.sylphian.minecraft.fishing.fish.Rarity
 * @see WeatherCondition
 */
public class LootService {

    private Map<Rarity, List<LootEntry>> poolsByRarity;
    private ConfigLoader config;
    private final Random random = new Random();

    /**
     * Constructs a new LootService.
     *
     * @param allFish the list of all available fish entries
     * @param config  the configuration loader for retrieving multipliers
     */
    public LootService(List<LootEntry> allFish, ConfigLoader config) {
        this.poolsByRarity = allFish.stream()
                .collect(Collectors.groupingBy(LootEntry::rarity));
        this.config = config;
    }

    /**
     * Convenience overload for {@link #rollCatch(Biome, WeatherCondition, double, long, List)}
     * with no active bait bonuses.
     *
     * @param biome     the biome where the fishing hook landed
     * @param weather   the current weather condition in the world
     * @param hookY     the Y coordinate of the fishing hook
     * @param worldTime the current world time in ticks (0-24000)
     * @return a CatchResult containing the fish ID, rarity, weight, and built ItemStack
     */
    public CatchResult rollCatch(Biome biome, WeatherCondition weather, double hookY, long worldTime) {
        return rollCatch(biome, weather, hookY, worldTime, List.of());
    }

    /**
     * Rolls a catch result for the given context.
     * First builds an eligible pool of all fish matching the biome,
     * Y coordinate, and time of day. Then rolls a rarity applying
     * weather and bait multipliers, and picks a fish from the matching pool.
     * If no fish of the rolled rarity exist in the eligible pool,
     * falls back to a weighted pick across the entire eligible pool.
     *
     * <p>Each bait config in {@code baitBonuses} must be a distinct type —
     * callers should use {@link BaitZoneService#getZonesAt} which already
     * enforces one zone per bait type.</p>
     *
     * @param biome       the biome where the fishing hook landed
     * @param weather     the current weather condition in the world
     * @param hookY       the Y coordinate of the fishing hook
     * @param worldTime   the current world time in ticks (0-24000)
     * @param baitBonuses bait zone bonuses to apply to rarity multipliers, empty for none
     * @return a CatchResult containing the fish ID, rarity, weight, and built ItemStack
     * @throws IllegalStateException if no fish are configured for the given context
     */
    public CatchResult rollCatch(Biome biome, WeatherCondition weather, double hookY, long worldTime, List<BaitConfig> baitBonuses) {
        List<LootEntry> eligiblePool = poolsByRarity.values().stream()
                .flatMap(List::stream)
                .filter(f -> f.appliesToBiome(biome))
                .filter(f -> f.appliesToY(hookY))
                .filter(f -> f.appliesToTime(worldTime))
                .toList();

        if (eligiblePool.isEmpty()) {
            throw new IllegalStateException("No fish configured for biome: " + biome.getKey().value()
                    + " at Y: " + (int) hookY + " at time: " + worldTime
                    + " - check loot_table.yml has fish covering this biome, height, and time.");
        }

        Rarity rolledRarity = rollRarity(weather, baitBonuses);

        List<LootEntry> rarityPool = eligiblePool.stream()
                .filter(f -> f.rarity().equals(rolledRarity))
                .toList();

        if (rarityPool.isEmpty()) {
            return buildCatchResult(weightedPickByRarity(eligiblePool, weather));
        }

        return buildCatchResult(weightedPick(rarityPool));
    }

    /**
     * Rolls a rarity based on chance, weather, and bait multipliers.
     * Each bait's multiplier for the rarity is multiplied together.
     *
     * @param weather     the current weather condition
     * @param baitBonuses bait bonuses to stack on top of weather multipliers, empty for none
     * @return the rolled rarity
     */
    private Rarity rollRarity(WeatherCondition weather, List<BaitConfig> baitBonuses) {
        double roll = random.nextDouble();

        for (Rarity rarity : Rarity.byDescendingRarity()) {
            double baseChance = rarity.getChance();
            double weatherMult = config.getWeatherMultiplier(weather, rarity);
            double baitMult = baitBonuses.stream()
                    .mapToDouble(b -> b.rarityMultipliers().getOrDefault(rarity.getId(), 1.0))
                    .reduce(1.0, (a, b) -> a * b);
            double finalChance = Math.min(1.0, baseChance * weatherMult * baitMult);

            if (roll <= finalChance) return rarity;
        }

        return Rarity.values().stream()
                .max(Comparator.comparingDouble(Rarity::getChance))
                .orElseThrow();
    }

    /**
     * Picks a fish from the eligible pool weighted by rarity chance.
     * Used when the rolled rarity has no fish in the current biome.
     *
     * @param pool    the eligible fish pool
     * @param weather the current weather condition
     * @return the selected fish entry
     */
    private LootEntry weightedPickByRarity(List<LootEntry> pool, WeatherCondition weather) {
        // Weight each fish by its rarity's effective chance
        double totalWeight = pool.stream()
                .mapToDouble(f -> {
                    double chance = f.rarity().getChance();
                    double multiplier = config.getWeatherMultiplier(weather, f.rarity());
                    return Math.min(1.0, chance * multiplier);
                })
                .sum();

        double roll = random.nextDouble() * totalWeight;
        double cursor = 0;

        for (LootEntry fish : pool) {
            double chance = fish.rarity().getChance();
            double multiplier = config.getWeatherMultiplier(weather, fish.rarity());
            cursor += Math.min(1.0, chance * multiplier);
            if (roll <= cursor) return fish;
        }

        return pool.getLast();
    }

    /**
     * Performs a weighted random selection from a pool of fish entries.
     *
     * @param pool the list of fish entries to pick from
     * @return the selected fish entry
     */
    private LootEntry weightedPick(List<LootEntry> pool) {
        int totalWeight = pool.stream().mapToInt(LootEntry::weight).sum();
        int roll = random.nextInt(totalWeight);
        int cursor = 0;

        // Iterate through the pool and accumulate weights until the roll is matched
        for (LootEntry fish : pool) {
            cursor += fish.weight();
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
    private CatchResult buildCatchResult(LootEntry fish) {
        double weight = fish.rollWeight(random);
        ItemStack itemStack = fish.externalItemId() == null ? buildItemStack(fish, weight) : null;
        return new CatchResult(weight, itemStack, fish);
    }

    /**
     * Rolls a rarity without selecting a fish.
     * Used by the bite timer system to determine wait time before a catch occurs.
     * The actual catch rolls rarity again independently.
     *
     * @param weather the current weather condition
     * @return the rolled rarity
     */
    public Rarity peekRarity(WeatherCondition weather) {
        double roll = random.nextDouble();

        for (Rarity rarity : Rarity.byDescendingRarity()) {
            double baseChance = rarity.getChance();
            double multiplier = config.getWeatherMultiplier(weather, rarity);
            double finalChance = Math.min(1.0, baseChance * multiplier);

            if (roll <= finalChance) return rarity;
        }

        return Rarity.values().stream()
                .max(Comparator.comparingDouble(Rarity::getChance))
                .orElseThrow();
    }

    /**
     * The PDC key stamped on every built fish ItemStack so other plugins can identify
     * it as a Sylphian-Fishing item. The value is the fish's entry ID, e.g. {@code "fish/common_cod"}.
     * Other plugins (e.g. Sylphian-Cooking) use this to match ingredients in recipes.
     */
    public static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("sylphian-fishing", "item_id");

    /**
     * Builds the ItemStack for a standard fish entry.
     * Stamps {@link #ITEM_ID_KEY} into the item's PDC so other plugins can identify it.
     * Must not be called for external item entries ({@link LootEntry#externalItemId()} non-null).
     *
     * @param fish         the fish entry
     * @param caughtWeight the rolled weight of the fish
     * @return the built ItemStack
     */
    public ItemStack buildItemStack(LootEntry fish, double caughtWeight) {
        ItemStack item = new ItemBuilder(fish.material())
                .name(fish.displayName())
                .loreStrings(buildLore(fish, caughtWeight))
                .build();
        item.editMeta(meta ->
                meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, "fish/" + fish.id())
        );
        return item;
    }

    /**
     * Builds the lore for the fish item, including description, rarity, and weight.
     *
     * @param fish         the fish entry
     * @param caughtWeight the rolled weight of the fish
     * @return a list of lore strings
     */
    private List<String> buildLore(LootEntry fish, double caughtWeight) {
        List<String> lore = new ArrayList<>();

        if (!fish.description().isEmpty()) {
            lore.addAll(Arrays.asList(fish.description().split("\n")));
            lore.add("");
        }

        lore.add(String.format("<gray>Rarity: %s", fish.rarity().getDisplayName()));
        lore.add(String.format("<gray>Weight: <white>%.2fkg", caughtWeight));

        return lore;
    }

    /**
     * Reloads the loot manager with updated configuration and fish definitions.
     * Rebuilds the internal rarity pool from the new fish list.
     *
     * @param config  the new configuration loader
     * @param allFish the updated list of all fish entries
     */
    public void reload(ConfigLoader config, List<LootEntry> allFish) {
        this.config = config;
        this.poolsByRarity = allFish.stream()
                .collect(Collectors.groupingBy(LootEntry::rarity));
    }

    /**
     * Returns the loot entry with the given ID, or empty if not found.
     *
     * @param id the entry ID to look up
     * @return the matching LootEntry, or empty
     */
    public Optional<LootEntry> getEntry(String id) {
        return poolsByRarity.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.id().equals(id))
                .findFirst();
    }

    /**
     * Returns all IDs of standard fish entries (those without an external item ID).
     * Used by {@link net.sylphian.minecraft.fishing.item.FishingItemProvider} to expose
     * available fish items to the cross-plugin item registry.
     *
     * @return the set of standard fish entry IDs
     */
    public Set<String> getItemEntryIds() {
        return poolsByRarity.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.externalItemId() == null)
                .map(LootEntry::id)
                .collect(Collectors.toSet());
    }

    /**
     * Builds the display ItemStack for the given entry with no catch weight.
     * Intended for item registry lookups rather than active catch delivery.
     *
     * @param entry the loot entry to build
     * @return the built ItemStack
     */
    public ItemStack buildItemStack(LootEntry entry) {
        return buildItemStack(entry, 0.0);
    }
}