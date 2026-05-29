package net.sylphian.minecraft.fishing.fish;

import java.util.*;

public class Rarity {
    private static final Map<String, Rarity> REGISTRY = new LinkedHashMap<>();

    private final String id;
    private final double chance;
    private final String color;
    private final double mutationMultiplier;

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

    public String getDisplayName() {
        String name = id.charAt(0) + id.substring(1).toLowerCase();
        return color + name;
    }

    public static void register(Rarity rarity) {
        REGISTRY.put(rarity.getId().toUpperCase(), rarity);
    }

    public static Rarity getById(String id) {
        return REGISTRY.get(id.toUpperCase());
    }

    public static Collection<Rarity> values() {
        return REGISTRY.values();
    }

    public static List<Rarity> byDescendingRarity() {
        return REGISTRY.values().stream()
                .sorted(Comparator.comparingDouble(Rarity::getChance).reversed())
                .toList();
    }

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
