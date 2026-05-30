package net.sylphian.minecraft.fishing.fish;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.List;

/**
 * Represents a single type of fish that can be caught through the fishing system.
 *
 * <p>Each fish defines its identity, appearance, and a set of optional restrictions
 * that control when and where it can appear in the loot pool. All restriction fields
 * are optional — omitting them means no restriction applies for that dimension.</p>
 *
 * <h2>Restriction Dimensions</h2>
 * <ul>
 *   <li><b>Biome</b> — list of biomes where the fish can appear, or empty for global</li>
 *   <li><b>Y Coordinate</b> — optional min/max Y range for the fishing hook position</li>
 *   <li><b>Time of Day</b> — optional min/max world time in ticks (0–24000)</li>
 * </ul>
 *
 * <h2>Time of Day Reference</h2>
 * <ul>
 *   <li>{@code 0} — Dawn</li>
 *   <li>{@code 6000} — Noon</li>
 *   <li>{@code 12000} — Dusk</li>
 *   <li>{@code 18000} — Midnight</li>
 * </ul>
 *
 * <p>Overnight time ranges are supported — if {@code minTime > maxTime}, the range
 * wraps around midnight. For example, {@code minTime: 18000, maxTime: 6000} means
 * the fish is catchable from dusk through to morning.</p>
 *
 * @see net.sylphian.minecraft.fishing.loot.LootManager
 * @see net.sylphian.minecraft.fishing.config.FishConfigLoader
 */
public class FishEntry {

    private final String id;
    private final Material material;
    private final String displayName;
    private final String description;
    private final Rarity rarity;
    private final int weight;
    private final List<Biome> biomes;
    private final double minWeight;
    private final double maxWeight;
    private final Integer minY;
    private final Integer maxY;
    private final Long minTime;
    private final Long maxTime;

    /**
     * Constructs a new FishEntry.
     *
     * @param id          unique identifier for the fish
     * @param material    the item material to use
     * @param displayName the MiniMessage formatted name displayed to players
     * @param description the MiniMessage formatted lore description
     * @param rarity      the fish rarity
     * @param weight      relative weight in the weighted loot pool
     * @param biomes      list of biomes where this fish can be caught, empty for no restriction
     * @param minWeight   minimum physical catch weight in kg
     * @param maxWeight   maximum physical catch weight in kg
     * @param minY        minimum Y coordinate to catch this fish, or null for no restriction
     * @param maxY        maximum Y coordinate to catch this fish, or null for no restriction
     * @param minTime     minimum world time in ticks to catch this fish, or null for no restriction
     * @param maxTime     maximum world time in ticks to catch this fish, or null for no restriction
     */
    public FishEntry(String id, Material material, String displayName, String description,
                     Rarity rarity, int weight, List<Biome> biomes,
                     double minWeight, double maxWeight,
                     Integer minY, Integer maxY,
                     Long minTime, Long maxTime) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.description = description;
        this.rarity = rarity;
        this.weight = weight;
        this.biomes = biomes;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        this.minY = minY;
        this.maxY = maxY;
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Rarity getRarity() { return rarity; }
    public int getWeight() { return weight; }
    public List<Biome> getBiomes() { return biomes; }
    public double getMinWeight() { return minWeight; }
    public double getMaxWeight() { return maxWeight; }
    public Integer getMinY() { return minY; }
    public Integer getMaxY() { return maxY; }
    public Long getMinTime() { return minTime; }
    public Long getMaxTime() { return maxTime; }

    /** Returns true if this fish has no biome restriction. */
    public boolean isGlobal() { return biomes.isEmpty(); }

    /**
     * Returns true if this fish has a Y coordinate restriction configured.
     *
     * @return true if minY or maxY is set
     */
    public boolean hasYRestriction() { return minY != null || maxY != null; }

    /**
     * Returns true if this fish has a time of day restriction configured.
     *
     * @return true if minTime or maxTime is set
     */
    public boolean hasTimeRestriction() { return minTime != null || maxTime != null; }

    /**
     * Checks if this fish can be caught in the specified biome.
     *
     * @param biome the biome to check
     * @return true if catchable in the biome, false otherwise
     */
    public boolean appliesToBiome(Biome biome) {
        // Empty list means it is a global fish
        return biomes.isEmpty() || biomes.contains(biome);
    }

    /**
     * Checks if this fish can be caught at the given Y coordinate.
     * If no Y restriction is configured, always returns true.
     *
     * @param y the Y coordinate of the fishing hook
     * @return true if catchable at this height
     */
    public boolean appliesToY(double y) {
        if (minY != null && y < minY) return false;
        if (maxY != null && y > maxY) return false;
        return true;
    }

    /**
     * Checks if this fish can be caught at the given Minecraft time of day.
     * Time is measured in ticks (0 = dawn, 6000 = noon, 12000 = dusk, 18000 = midnight).
     * Supports overnight ranges where minTime is greater than maxTime
     * e.g. minTime: 18000, maxTime: 6000 means dusk through to morning.
     *
     * @param worldTime the current world time in ticks
     * @return true if catchable at this time
     */
    public boolean appliesToTime(long worldTime) {
        if (minTime == null && maxTime == null) return true;

        long min = minTime != null ? minTime : 0;
        long max = maxTime != null ? maxTime : 24000;

        // Handle overnight wrap-around e.g. 18000 → 6000
        if (min > max) {
            return worldTime >= min || worldTime <= max;
        }

        return worldTime >= min && worldTime <= max;
    }

    /**
     * Rolls a random physical weight for the fish based on its min/max range.
     *
     * @param random the random source
     * @return the rolled weight in kg
     */
    public double rollWeight(java.util.Random random) {
        return minWeight + (maxWeight - minWeight) * random.nextDouble();
    }
}