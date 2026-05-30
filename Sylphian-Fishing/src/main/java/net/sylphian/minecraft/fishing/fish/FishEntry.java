package net.sylphian.minecraft.fishing.fish;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.List;

/**
 * Represents a single type of fish that can be caught.
 * Defines the fish's properties, including its rarity, relative weight in the loot pool,
 * biome restrictions, and physical weight range.
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

    /**
     * Constructs a new FishEntry.
     *
     * @param id          unique identifier for the fish
     * @param material    the item material to use
     * @param displayName the name displayed to players
     * @param description the description/lore of the fish
     * @param rarity      the fish rarity
     * @param weight      relative weight in the weighted pool
     * @param biomes      list of biomes where this fish can be caught
     * @param minWeight   minimum catch weight
     * @param maxWeight   maximum catch weight
     */
    public FishEntry(String id, Material material, String displayName, String description,
                     Rarity rarity, int weight, List<Biome> biomes,
                     double minWeight, double maxWeight) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.description = description;
        this.rarity = rarity;
        this.weight = weight;
        this.biomes = biomes;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
    }

    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Rarity getRarity() { return rarity; }
    public int getWeight() { return weight; }
    public List<Biome> getBiomes() { return biomes; }
    /**
     *  Catchable anywhere
     */
    public boolean isGlobal() { return biomes.isEmpty(); }
    public double getMinWeight() { return minWeight; }
    public double getMaxWeight() { return maxWeight; }

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
     * Rolls a random physical weight for the fish based on its min/max range.
     *
     * @param random the random source
     * @return the rolled weight
     */
    public double rollWeight(java.util.Random random) {
        return minWeight + (maxWeight - minWeight) * random.nextDouble();
    }
}