package net.sylphian.minecraft.gathering.node;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The tool a node requires: a category (pickaxe, axe, etc.) and a minimum tier.
 * A {@link ToolCategory#ANY} requirement matches anything, including a bare hand.
 *
 * @param category the required tool category
 * @param minTier  the minimum tool tier, compared by ordinal
 */
public record ToolRequirement(ToolCategory category, ToolTier minTier) {

    /** Tool families a node can require. {@code ANY} imposes no requirement. */
    public enum ToolCategory {
        PICKAXE,
        AXE,
        SHOVEL,
        HOE,
        ANY
    }

    /**
     * Vanilla tool tiers in ascending strength.
     */
    public enum ToolTier {
        WOOD,
        GOLD,
        STONE,
        IRON,
        DIAMOND,
        NETHERITE
    }

    /**
     * Returns whether the held item satisfies this requirement.
     *
     * @param held the item in the player's hand, may be null
     * @return true if the requirement is {@code ANY}, or the item is of the right
     *         category and its tier is at least {@link #minTier}
     */
    public boolean matches(@Nullable ItemStack held) {
        if (category == ToolCategory.ANY) return true;
        if (held == null) return false;

        Material material = held.getType();
        if (categoryOf(material) != category) return false;

        ToolTier tier = tierOf(material);
        return tier != null && tier.ordinal() >= minTier.ordinal();
    }

    private static @Nullable ToolCategory categoryOf(Material material) {
        String name = material.name();
        if (name.endsWith("_PICKAXE")) return ToolCategory.PICKAXE;
        if (name.endsWith("_AXE")) return ToolCategory.AXE;
        if (name.endsWith("_SHOVEL")) return ToolCategory.SHOVEL;
        if (name.endsWith("_HOE")) return ToolCategory.HOE;
        return null;
    }

    private static @Nullable ToolTier tierOf(Material material) {
        String name = material.name();
        if (name.startsWith("WOODEN_")) return ToolTier.WOOD;
        if (name.startsWith("GOLDEN_")) return ToolTier.GOLD;
        if (name.startsWith("STONE_")) return ToolTier.STONE;
        if (name.startsWith("IRON_")) return ToolTier.IRON;
        if (name.startsWith("DIAMOND_")) return ToolTier.DIAMOND;
        if (name.startsWith("NETHERITE_")) return ToolTier.NETHERITE;
        return null;
    }
}
