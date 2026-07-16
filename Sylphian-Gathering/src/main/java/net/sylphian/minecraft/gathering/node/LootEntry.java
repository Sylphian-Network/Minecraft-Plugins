package net.sylphian.minecraft.gathering.node;

import java.util.Random;

/**
 * One weighted row of a node's loot table: an item reference and an amount range.
 *
 * @param itemId    the item reference in {@code "namespace:id"} form, resolved via the item registry
 * @param weight    the relative pick weight; higher is more common
 * @param minAmount the minimum amount, inclusive
 * @param maxAmount the maximum amount, inclusive
 */
public record LootEntry(String itemId, int weight, int minAmount, int maxAmount) {

    /**
     * Rolls a random amount in {@code [minAmount, maxAmount]}.
     *
     * @param random the source of randomness
     * @return the rolled amount
     */
    public int rollAmount(Random random) {
        if (maxAmount <= minAmount) return Math.max(0, minAmount);
        return minAmount + random.nextInt(maxAmount - minAmount + 1);
    }
}
