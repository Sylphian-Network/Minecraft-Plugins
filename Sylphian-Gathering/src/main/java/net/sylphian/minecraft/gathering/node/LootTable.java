package net.sylphian.minecraft.gathering.node;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * A weighted table of loot entries a node draws from on each harvest.
 *
 * @param entries the weighted entries; may be empty
 */
public record LootTable(List<LootEntry> entries) {

    public LootTable {
        entries = List.copyOf(entries);
    }

    /**
     * Rolls one entry by weight.
     *
     * @param random the source of randomness
     * @return the picked entry, or null if the table is empty
     */
    public @Nullable LootEntry roll(Random random) {
        if (entries.isEmpty()) return null;

        int totalWeight = 0;
        for (LootEntry entry : entries) totalWeight += Math.max(0, entry.weight());
        if (totalWeight <= 0) return entries.getFirst();

        int roll = random.nextInt(totalWeight);
        for (LootEntry entry : entries) {
            roll -= Math.max(0, entry.weight());
            if (roll < 0) return entry;
        }
        return entries.getLast();
    }
}
