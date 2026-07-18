package net.sylphian.minecraft.gathering.world;

import net.sylphian.minecraft.gathering.config.GatheringConfig.EffectSettings;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Outlines each available node with a glow so players can tell a node from an
 * ordinary block. The glow comes from a block display of the node's own block,
 * tucked just inside the real block so only the outline shows. Purely cosmetic:
 * client-rendered display entities with no per-tick server cost, driven off
 * node state by {@link NodeManager}.
 */
final class NodeMarkers {

    static final String MARKER_TAG = "sylphian_gathering_marker";

    private static final Color DEFAULT_COLOR = Color.WHITE;
    private static final float REPLENISH_PEAK_SCALE = 1.25f;
    private static final int REPLENISH_DURATION_TICKS = 8;

    private final Plugin plugin;
    private EffectSettings settings;
    private final Map<NodeManager.NodeKey, BlockDisplay> markers = new HashMap<>();

    NodeMarkers(Plugin plugin, EffectSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /** Swaps in reloaded settings; the caller re-syncs every node. */
    void reload(EffectSettings settings) {
        this.settings = settings;
    }

    /**
     * Shows the marker for an available node and hides it otherwise. Assumes the
     * node's chunk is loaded (its only caller guards on that).
     *
     * @param node the node whose marker should match its current state
     */
    void sync(LiveNode node) {
        if (settings.enabled() && node.state() == LiveNode.State.AVAILABLE) {
            show(node);
        } else {
            remove(node);
        }
    }

    /**
     * Pops the node's glow outward and eases it back to rest, signalling that the
     * node has replenished. No-op when markers are disabled or the node has no live
     * marker (e.g. its chunk is unloaded).
     *
     * @param node the node that just replenished
     */
    void playReplenish(LiveNode node) {
        if (!settings.enabled()) return;
        BlockDisplay display = markers.get(keyOf(node));
        if (display == null || !display.isValid()) return;

        display.setInterpolationDelay(0);
        display.setInterpolationDuration(0);
        display.setTransformation(transformationForScale(REPLENISH_PEAK_SCALE));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(REPLENISH_DURATION_TICKS);
            display.setTransformation(tuckedTransformation());
        }, 1L);
    }

    /** Removes and untracks the marker for one node, if present. */
    void remove(LiveNode node) {
        BlockDisplay display = markers.remove(keyOf(node));
        if (display != null) display.remove();
    }

    /** Drops marker references for a chunk being unloaded, removing the entities. */
    void forget(Collection<LiveNode> chunkNodes) {
        for (LiveNode node : chunkNodes) remove(node);
    }

    /** Removes every tracked marker and sweeps any leftover tagged displays in loaded worlds. */
    void removeAll() {
        for (BlockDisplay display : markers.values()) {
            if (display != null) display.remove();
        }
        markers.clear();
        sweepOrphans();
    }

    private void show(LiveNode node) {
        Material material = node.currentBlock();
        Color color = colorFor(node);
        Transformation transformation = tuckedTransformation();

        BlockDisplay existing = markers.get(keyOf(node));
        if (existing != null && existing.isValid()) {
            existing.setBlock(material.createBlockData());
            existing.setGlowColorOverride(color);
            existing.setTransformation(transformation);
            return;
        }

        BlockDisplay display = node.world().spawn(
                new Location(node.world(), node.x(), node.y(), node.z()), BlockDisplay.class, d -> {
                    d.setBlock(material.createBlockData());
                    d.setGlowing(true);
                    d.setGlowColorOverride(color);
                    // Bukkit renders a display within roughly viewRange * 64 blocks.
                    d.setViewRange(settings.viewRange() / 64.0f);
                    d.setPersistent(false);
                    d.setTransformation(transformation);
                    d.addScoreboardTag(MARKER_TAG);
                });
        markers.put(keyOf(node), display);
    }

    private Transformation tuckedTransformation() {
        return transformationForScale(settings.scale());
    }

    private Transformation transformationForScale(float scale) {
        float inset = (1.0f - scale) / 2.0f;
        return new Transformation(
                new Vector3f(inset, inset, inset), new AxisAngle4f(),
                new Vector3f(scale, scale, scale), new AxisAngle4f());
    }

    private Color colorFor(LiveNode node) {
        String skillId = node.type().skillId();
        if (skillId != null) {
            Color color = settings.colors().get(skillId);
            if (color != null) return color;
        }
        return DEFAULT_COLOR;
    }

    private void sweepOrphans() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(BlockDisplay.class)) {
                if (entity.getScoreboardTags().contains(MARKER_TAG)) entity.remove();
            }
        }
    }

    private NodeManager.NodeKey keyOf(LiveNode node) {
        return new NodeManager.NodeKey(node.world().getKey().asString(), node.x(), node.y(), node.z());
    }
}
