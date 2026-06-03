package net.sylphian.minecraft.fishing.fish;

import org.bukkit.inventory.ItemStack;

/**
 * Represents the result of a fishing catch attempt.
 *
 * @param weight    the rolled physical weight in kilograms
 * @param itemStack the built item for {@link LootEntryType#ITEM} entries;
 *                  {@code null} for {@link LootEntryType#CRATE_KEY} entries
 * @param entry     the originating loot entry
 */
public record CatchResult(double weight, ItemStack itemStack, LootEntry entry) {

    /**
     * Returns the unique ID of the caught entry. Convenience accessor for {@link LootEntry#id()}.
     *
     * @return the entry ID
     */
    public String fishId() { return entry.id(); }

    /**
     * Returns the rarity of the caught entry. Convenience accessor for {@link LootEntry#rarity()}.
     *
     * @return the entry rarity
     */
    public Rarity rarity() { return entry.rarity(); }
}