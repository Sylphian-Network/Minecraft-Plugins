package net.sylphian.minecraft.gathering.node;

import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A rollable variant applied to a live node, e.g. a "rich" or "pristine" vein
 * that yields more and shows a different block. Rolled on placement and on each
 * respawn.
 *
 * @param id              the modifier id, for display and tracing
 * @param chance          the probability this modifier is picked, 0.0 to 1.0
 * @param yieldMultiplier multiplies each base loot amount
 * @param bonusLoot       extra loot rolled in addition to the base table
 * @param blockOverride   the block shown while this modifier is active, or null to keep the node's available block
 */
public record NodeModifier(
        String id,
        double chance,
        double yieldMultiplier,
        List<LootEntry> bonusLoot,
        @Nullable Material blockOverride) {

    public NodeModifier {
        bonusLoot = List.copyOf(bonusLoot);
    }
}
