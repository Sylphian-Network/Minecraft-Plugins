package net.sylphian.minecraft.gathering.node;

/**
 * One node placement declared in the engine's config: a node type id and the
 * block it sits at. The {@code nodeId} is an opaque {@code "namespace:id"}
 * string resolved against {@link net.sylphian.minecraft.gathering.registry.GatheringNodeRegistry}.
 *
 * @param nodeId the node type reference, e.g. {@code "sylphian-mining:iron_vein"}
 * @param x      the block X coordinate
 * @param y      the block Y coordinate
 * @param z      the block Z coordinate
 */
public record NodePlacement(String nodeId, int x, int y, int z) {
}
