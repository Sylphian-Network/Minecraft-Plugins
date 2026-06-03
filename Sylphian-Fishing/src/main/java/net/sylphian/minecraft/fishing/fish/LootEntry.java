package net.sylphian.minecraft.fishing.fish;

import net.sylphian.minecraft.fishing.config.LootTableConfigLoader;
import net.sylphian.minecraft.fishing.services.LootService;
import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.List;
import java.util.Random;

/**
 * Represents a single entry in the fishing loot table.
 *
 * <p>An entry is either a {@link LootEntryType#ITEM} (a standard fish or item) or a
 * {@link LootEntryType#CRATE_KEY} (a Sylphian Crates key delivered via the CratesAPI).
 * Item-type entries carry a material, display name, and description. Crate key entries
 * carry only a {@code keyId} — display and material fields will be null.</p>
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
 * @see LootService
 * @see LootTableConfigLoader
 */
public record LootEntry(String id, LootEntryType type, String keyId,
                        Material material, String displayName, String description,
                        Rarity rarity, int weight, List<Biome> biomes,
                        double minWeight, double maxWeight,
                        Integer minY, Integer maxY, Long minTime, Long maxTime) {

    /**
     * Constructs a new LootEntry.
     *
     * @param id          unique identifier for the entry
     * @param type        the reward type — determines how the catch is delivered
     * @param keyId       the crate key ID to give; only populated when type is {@link LootEntryType#CRATE_KEY}
     * @param material    the item material to use; only populated when type is {@link LootEntryType#ITEM}
     * @param displayName the MiniMessage formatted name displayed to players; only populated when type is {@link LootEntryType#ITEM}
     * @param description the MiniMessage formatted lore description; only populated when type is {@link LootEntryType#ITEM}
     * @param rarity      the entry rarity
     * @param weight      relative weight in the weighted loot pool
     * @param biomes      list of biomes where this entry can be caught, empty for no restriction
     * @param minWeight   minimum physical catch weight in kg
     * @param maxWeight   maximum physical catch weight in kg
     * @param minY        minimum Y coordinate to catch this entry, or null for no restriction
     * @param maxY        maximum Y coordinate to catch this entry, or null for no restriction
     * @param minTime     minimum world time in ticks to catch this entry, or null for no restriction
     * @param maxTime     maximum world time in ticks to catch this entry, or null for no restriction
     */
    public LootEntry {
    }

    /**
     * Returns true if this fish has no biome restriction.
     */
    public boolean isGlobal() {
        return biomes.isEmpty();
    }

    /**
     * Returns true if this fish has a Y coordinate restriction configured.
     *
     * @return true if minY or maxY is set
     */
    public boolean hasYRestriction() {
        return minY != null || maxY != null;
    }

    /**
     * Returns true if this fish has a time of day restriction configured.
     *
     * @return true if minTime or maxTime is set
     */
    public boolean hasTimeRestriction() {
        return minTime != null || maxTime != null;
    }

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
    public double rollWeight(Random random) {
        return minWeight + (maxWeight - minWeight) * random.nextDouble();
    }
}