package net.sylphian.minecraft.fishing.fish;

/**
 * Defines the types of entries that can appear in the fishing loot table.
 */
public enum LootEntryType {
    /** Gives the player a standard item built from the entry's material. */
    ITEM,
    /** Gives the player a Sylphian Crates key via the CratesAPI. */
    CRATE_KEY
}