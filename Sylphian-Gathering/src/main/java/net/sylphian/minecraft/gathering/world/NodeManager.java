package net.sylphian.minecraft.gathering.world;

import net.sylphian.minecraft.gathering.config.GatheringConfig;
import net.sylphian.minecraft.gathering.node.NodePlacement;
import net.sylphian.minecraft.gathering.node.NodeType;
import net.sylphian.minecraft.gathering.registry.GatheringNodeRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Owns every {@link LiveNode}, resolving placements from the engine config to
 * live blocks and keeping their visuals in sync as chunks load.
 */
public final class NodeManager implements Listener {

    /** Identity of a placed node: its world key and block coordinates. */
    public record NodeKey(String worldKey, int x, int y, int z) {}

    /** Identity of a chunk: its world key and chunk coordinates. */
    private record ChunkKey(String worldKey, int cx, int cz) {}

    private final Plugin plugin;
    private final Logger logger;
    private final Random random = new Random();
    private final Map<NodeKey, LiveNode> nodes = new HashMap<>();
    private final Map<ChunkKey, List<LiveNode>> nodesByChunk = new HashMap<>();
    private final NodeMarkers markers;

    private GatheringConfig config;

    public NodeManager(Plugin plugin, GatheringConfig config) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = config;
        this.markers = new NodeMarkers(plugin, config.markers());
    }

    /**
     * Swaps in a reloaded config so the next {@link #resolve()} reads the new placements.
     *
     * @param newConfig the reloaded config
     */
    public void reload(GatheringConfig newConfig) {
        this.config = newConfig;
        this.markers.reload(newConfig.markers());
    }

    /**
     * Clears and rebuilds every live node from the engine config placements.
     * Unknown node ids and unloaded worlds are skipped and logged. Available
     * blocks are asserted for any chunk already loaded; the rest are asserted by
     * {@link #onChunkLoad} when their chunk loads.
     */
    public void resolve() {
        restoreAll();
        nodes.clear();
        nodesByChunk.clear();
        markers.removeAll();

        int resolved = 0;
        for (Map.Entry<String, List<NodePlacement>> entry : config.placements().entrySet()) {
            String worldKey = entry.getKey();
            List<NodePlacement> placements = entry.getValue();

            NamespacedKey key = NamespacedKey.fromString(worldKey);
            World world = key == null ? null : Bukkit.getWorld(key);
            if (world == null) {
                logger.warning("World '" + worldKey + "' is not loaded; skipping its " + placements.size() + " gathering node(s).");
                continue;
            }

            int worldResolved = 0;
            for (NodePlacement placement : placements) {
                NodeType type = GatheringNodeRegistry.get(placement.nodeId()).orElse(null);
                if (type == null) {
                    logger.warning("World '" + worldKey + "' references unknown gathering node '" + placement.nodeId() + "'; skipping.");
                    continue;
                }

                LiveNode node = new LiveNode(type, world, placement.x(), placement.y(), placement.z());
                node.setActiveModifier(type.rollModifier(random));
                index(node);
                applyBlockState(node);
                worldResolved++;
            }

            if (worldResolved > 0) {
                logger.info("Resolved " + worldResolved + " gathering node(s) in world '" + worldKey + "'.");
            }
            resolved += worldResolved;
        }
        logger.info("Gathering: " + resolved + " node(s) live across all worlds.");
    }

    /**
     * Returns the live node at the given block, if any.
     *
     * @param block the block to look up
     * @return the live node, or null if the block is not a node
     */
    public @Nullable LiveNode lookup(Block block) {
        return nodes.get(new NodeKey(block.getWorld().getKey().asString(), block.getX(), block.getY(), block.getZ()));
    }

    /**
     * @return every live node, for admin tooling
     */
    public Collection<LiveNode> liveNodes() {
        return nodes.values();
    }

    /**
     * Re-rolls the node's modifier, returns it to available, and re-asserts its
     * block. Shared by the respawn scheduler and by content plugins refreshing a
     * node through {@code GatheringNodeService}. Does not check the current state.
     *
     * @param node the node to refresh
     */
    public void refresh(LiveNode node) {
        node.setActiveModifier(node.type().rollModifier(random));
        node.setState(LiveNode.State.AVAILABLE);
        applyBlockState(node);
    }

    /**
     * Sets the node's block to match its current state and modifier, if the chunk
     * is loaded and the block is not already correct. Physics updates are
     * suppressed to avoid cascading changes.
     *
     * @param node the node to assert
     */
    public void applyBlockState(LiveNode node) {
        World world = node.world();
        if (!world.isChunkLoaded(node.x() >> 4, node.z() >> 4)) return;
        Block block = world.getBlockAt(node.x(), node.y(), node.z());
        Material desired = node.currentBlock();
        if (block.getType() != desired) block.setType(desired, false);
        markers.sync(node);
    }

    /**
     * Re-asserts the blocks of any nodes in a chunk when it loads, so visuals
     * survive chunk unload/reload. A chunk with no nodes costs one map miss; the
     * write is deferred one tick so the world is not mutated from inside the load.
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        ChunkKey key = new ChunkKey(event.getWorld().getKey().asString(),
                event.getChunk().getX(), event.getChunk().getZ());
        if (!nodesByChunk.containsKey(key)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            List<LiveNode> inChunk = nodesByChunk.get(key);
            if (inChunk == null) return;
            for (LiveNode node : inChunk) applyBlockState(node);
        });
    }

    /**
     * Drops marker references for a chunk being unloaded. The display entities are
     * non-persistent and removed by the server on unload; this clears the stale
     * references so they are re-spawned cleanly when the chunk loads again.
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        ChunkKey key = new ChunkKey(event.getWorld().getKey().asString(), event.getChunk().getX(), event.getChunk().getZ());
        List<LiveNode> inChunk = nodesByChunk.get(key);
        if (inChunk != null) markers.forget(inChunk);
    }

    /**
     * Restores every depleted node to its available block and clears the indexes.
     * Call from onDisable.
     */
    public void clearAll() {
        restoreAll();
        nodes.clear();
        nodesByChunk.clear();
        markers.removeAll();
    }

    private void restoreAll() {
        for (LiveNode node : nodes.values()) {
            if (node.state() != LiveNode.State.DEPLETED) continue;
            World world = node.world();
            if (!world.isChunkLoaded(node.x() >> 4, node.z() >> 4)) continue;
            world.getBlockAt(node.x(), node.y(), node.z()).setType(node.type().availableBlock(), false);
        }
    }

    private void index(LiveNode node) {
        String worldKey = node.world().getKey().asString();
        nodes.put(new NodeKey(worldKey, node.x(), node.y(), node.z()), node);
        nodesByChunk.computeIfAbsent(new ChunkKey(worldKey, node.x() >> 4, node.z() >> 4),
                k -> new ArrayList<>()).add(node);
    }
}
