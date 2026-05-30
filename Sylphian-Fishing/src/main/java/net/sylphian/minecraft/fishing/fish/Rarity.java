package net.sylphian.minecraft.fishing.fish;

import java.util.*;

/**
 * Represents a catch rarity level (e.g., COMMON, RARE, LEGENDARY).
 * Each rarity has an associated base catch chance and color for display.
 * Holds mutation multipliers to influence secondary catch effects.
 */
public class Rarity {
    private static final Map<String, Rarity> REGISTRY = new LinkedHashMap<>();

    private final String id;
    private final double chance;
    private final String color;
    private final double mutationMultiplier;

    /**
     * Constructs a new Rarity.
     *
     * @param id                 unique identifier for the rarity
     * @param chance             base catch chance (0.0 to 1.0)
     * @param color              MiniMessage color tag for display
     * @param mutationMultiplier multiplier for mutation occurrences
     */
    public Rarity(String id, double chance, String color, double mutationMultiplier) {
        this.id = id;
        this.chance = chance;
        this.color = color;
        this.mutationMultiplier = mutationMultiplier;
    }

    public String getId() { return id; }
    public double getChance() { return chance; }
    public String getColor() { return color; }
    public double getMutationMultiplier() { return mutationMultiplier; }

    /**
     * Gets the formatted display name of the rarity, including its color.
     *
     * @return the formatted display name
     */
    public String getDisplayName() {
        String name = id.charAt(0) + id.substring(1).toLowerCase();
        return color + name;
    }

    /**
     * Registers a rarity in the global registry.
     *
     * @param rarity the rarity to register
     */
    public static void register(Rarity rarity) {
        REGISTRY.put(rarity.getId().toUpperCase(), rarity);
    }

    /**
     * Retrieves a rarity by its ID.
     *
     * @param id the ID of the rarity
     * @return the rarity, or null if not found
     */
    public static Rarity getById(String id) {
        return REGISTRY.get(id.toUpperCase());
    }

    /**
     * Gets all registered rarities.
     *
     * @return a collection of all rarities
     */
    public static Collection<Rarity> values() {
        return REGISTRY.values();
    }

    /**
     * Gets all registered rarities sorted by their catch chance in descending order
     * (from rarest/smallest chance to most common/largest chance).
     *
     * @return a sorted list of rarities
     */
    public static List<Rarity> byDescendingRarity() {
        return REGISTRY.values().stream()
                .sorted(Comparator.comparingDouble(Rarity::getChance).reversed())
                .toList();
    }

    /**
     * Clears the rarity registry.
     */
    public static void clear() {
        REGISTRY.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rarity rarity = (Rarity) o;
        return Objects.equals(id.toUpperCase(), rarity.id.toUpperCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id.toUpperCase());
    }

    @Override
    public String toString() {
        return id.toUpperCase();
    }
}
