package net.sylphian.minecraft.gathering.registry;

import net.sylphian.minecraft.gathering.node.NodeType;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry of node types contributed by content plugins.
 */
public final class GatheringNodeRegistry {

    private static final ConcurrentHashMap<String, NodeType> types = new ConcurrentHashMap<>();
    private static volatile @Nullable Runnable changeListener;

    private GatheringNodeRegistry() {}

    /**
     * Sets the callback fired whenever the registered node types change, so the
     * engine can re-resolve placements. Set by Sylphian-Gathering onEnable and
     * cleared with null on disable.
     *
     * @param listener the change callback, or null to clear
     */
    public static void setChangeListener(@Nullable Runnable listener) {
        changeListener = listener;
    }

    /**
     * Registers a node type, replacing any existing type with the same id.
     *
     * @param type the node type to register
     */
    public static void register(NodeType type) {
        types.put(type.id(), type);
        fireChanged();
    }

    /**
     * Registers a collection of node types.
     *
     * @param nodeTypes the node types to register
     */
    public static void register(Collection<NodeType> nodeTypes) {
        for (NodeType type : nodeTypes) types.put(type.id(), type);
        fireChanged();
    }

    /**
     * Removes every node type whose id begins with {@code namespace + ":"}.
     * Call from the owning content plugin's onDisable.
     *
     * @param namespace the namespace to clear
     */
    public static void unregister(String namespace) {
        String prefix = namespace + ":";
        types.keySet().removeIf(id -> id.startsWith(prefix));
        fireChanged();
    }

    /**
     * Returns the node type for the given id.
     *
     * @param id the node type id in {@code "namespace:id"} form
     * @return the node type, or empty if not registered
     */
    public static Optional<NodeType> get(String id) {
        return Optional.ofNullable(types.get(id));
    }

    /**
     * @return every registered node type id
     */
    public static Set<String> ids() {
        return Set.copyOf(types.keySet());
    }

    private static void fireChanged() {
        Runnable listener = changeListener;
        if (listener != null) listener.run();
    }
}
