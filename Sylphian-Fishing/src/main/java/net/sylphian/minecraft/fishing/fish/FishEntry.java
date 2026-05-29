package net.sylphian.minecraft.fishing.fish;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.List;

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

    public boolean appliesToBiome(Biome biome) {
        return biomes.isEmpty() || biomes.contains(biome);
    }

    public double rollWeight(java.util.Random random) {
        return minWeight + (maxWeight - minWeight) * random.nextDouble();
    }
}