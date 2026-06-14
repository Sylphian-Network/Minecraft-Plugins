package net.sylphian.minecraft.crates.config;

/**
 * Defines the types of rewards that can be granted when a crate is opened.
 */
public enum RewardType {
    /** Gives the player a physical ItemStack. */
    ITEM,
    /** Deposits money into the player's balance. Requires Sylphian-Economy; ignored if absent. */
    MONEY,
}