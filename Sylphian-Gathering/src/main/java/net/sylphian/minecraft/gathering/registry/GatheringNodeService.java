package net.sylphian.minecraft.gathering.registry;

import net.sylphian.minecraft.gathering.world.LiveNode;
import net.sylphian.minecraft.gathering.world.NodeManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Exported read/refresh accessor over the engine's live nodes, for content
 * plugins that need to inspect or restore nodes (e.g. ability activation
 * predicates and capstone refresh abilities).
 *
 * <p>Deliberately exposes no {@code harvest} method: harvesting through here
 * would re-fire {@code NodeHarvestEvent} and recurse. Set up in
 * {@code SylphianGathering.onEnable} and cleared in {@code onDisable}.</p>
 */
public final class GatheringNodeService {

    private static volatile @Nullable NodeManager manager;

    private GatheringNodeService() {}

    /**
     * Binds the engine's node manager. Engine-owned; called from onEnable.
     *
     * @param nodeManager the live node manager
     */
    public static void init(NodeManager nodeManager) {
        manager = nodeManager;
    }

    /** Clears the binding. Called from onDisable. */
    public static void shutdown() {
        manager = null;
    }

    /**
     * Looks up the live node at a block.
     *
     * @param block the block to look up
     * @return the live node, or empty if the block is not a node or the service is down
     */
    public static Optional<LiveNode> lookup(Block block) {
        NodeManager m = manager;
        if (m == null) return Optional.empty();
        return Optional.ofNullable(m.lookup(block));
    }

    /**
     * Returns whether the block is a node owned by the given skill.
     *
     * @param block   the block to test
     * @param skillId the skill id to match against the node type
     * @return true if the block is a node whose type awards the given skill
     */
    public static boolean isNodeFor(Block block, String skillId) {
        return lookup(block)
                .map(node -> skillId.equals(node.type().skillId()))
                .orElse(false);
    }

    /**
     * Returns every live node within {@code radius} blocks of a centre, in the
     * same world.
     *
     * @param centre the centre location
     * @param radius the search radius in blocks
     * @return the nodes in range, empty if the service is down
     */
    public static Collection<LiveNode> nearby(Location centre, int radius) {
        NodeManager m = manager;
        if (m == null) return List.of();

        World world = centre.getWorld();
        if (world == null) return List.of();

        double radiusSquared = (double) radius * radius;
        List<LiveNode> found = new ArrayList<>();
        for (LiveNode node : m.liveNodes()) {
            if (!node.world().equals(world)) continue;
            double dx = node.x() + 0.5 - centre.getX();
            double dy = node.y() + 0.5 - centre.getY();
            double dz = node.z() + 0.5 - centre.getZ();
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) found.add(node);
        }
        return found;
    }

    /**
     * Re-rolls a node's modifier, returns it to available, and re-asserts its
     * block. No-op if the service is down.
     *
     * @param node the node to refresh
     */
    public static void refresh(LiveNode node) {
        NodeManager m = manager;
        if (m != null) m.refresh(node);
    }
}
