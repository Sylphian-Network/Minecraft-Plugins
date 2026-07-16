package net.sylphian.minecraft.gathering.node;

import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * An immutable resource-node definition registered by a content plugin. Node
 * placements in the Dimensions config reference these by {@link #id()}.
 *
 * @param id            the node type id in {@code "namespace:id"} form
 * @param interaction   how the node is harvested
 * @param availableBlock the block shown while the node is available
 * @param depletedBlock the block shown while the node is depleted
 * @param tool          the tool required to harvest, or null for no requirement
 * @param respawnSeconds seconds before a depleted node returns; 0 defers to the engine default
 * @param skillId       the skill to award XP in, or null for none
 * @param xp            XP awarded per harvest
 * @param minSkillLevel the minimum level in {@link #skillId} required to harvest; 0 for none
 * @param loot          the base loot table
 * @param modifiers     the rollable modifiers, checked in order
 */
public record NodeType(
        String id,
        NodeInteraction interaction,
        Material availableBlock,
        Material depletedBlock,
        @Nullable ToolRequirement tool,
        int respawnSeconds,
        @Nullable String skillId,
        long xp,
        int minSkillLevel,
        LootTable loot,
        List<NodeModifier> modifiers) {

    public NodeType {
        modifiers = List.copyOf(modifiers);
    }

    /**
     * Rolls a modifier for a fresh node. Each modifier's {@code chance} is
     * checked in order and the first hit wins; otherwise no modifier applies.
     *
     * @param random the source of randomness
     * @return the rolled modifier, or null for the plain node
     */
    public @Nullable NodeModifier rollModifier(Random random) {
        for (NodeModifier modifier : modifiers) {
            if (random.nextDouble() < modifier.chance()) return modifier;
        }
        return null;
    }
}
