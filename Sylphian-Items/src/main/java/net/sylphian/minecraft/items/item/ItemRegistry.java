package net.sylphian.minecraft.items.item;

import org.bukkit.inventory.ItemStack;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for cross-plugin custom items.
 *
 * <p>Plugins register an {@link ItemProvider} on enable and unregister on disable.
 * Items are referenced by a namespaced ID in the form {@code namespace:item-id},
 * e.g. {@code "sylphian-crates:legendary_key"} or {@code "sylphian-fishing:bait/ocean_bait"}.</p>
 *
 * <p>The registry is thread-safe. Providers are resolved lazily — items are built
 * only when requested, so registration has no per-item cost regardless of pool size.</p>
 */
public final class ItemRegistry {

    private static final ConcurrentHashMap<String, ItemProvider> providers = new ConcurrentHashMap<>();
    private static Logger logger;

    private ItemRegistry() {}

    /**
     * Sets the logger used for warnings. Call once from Sylphian-Items onEnable.
     *
     * @param log the logger to use
     */
    public static void init(Logger log) {
        logger = log;
    }

    /**
     * Registers an item provider under its declared namespace.
     * Replaces any existing provider with the same namespace.
     *
     * @param provider the provider to register
     */
    public static void register(ItemProvider provider) {
        providers.put(provider.namespace(), provider);
    }

    /**
     * Unregisters the provider for the given namespace.
     * Call from the owning plugin's onDisable.
     *
     * @param namespace the namespace to remove
     */
    public static void unregister(String namespace) {
        providers.remove(namespace);
    }

    /**
     * Builds and returns the item for the given namespaced ID.
     *
     * @param namespacedId the item reference in the form {@code "namespace:item-id"}
     * @return the built ItemStack, or empty if the namespace or item ID is not found
     */
    public static Optional<ItemStack> get(String namespacedId) {
        int colon = namespacedId.indexOf(':');
        if (colon == -1) {
            warn("Invalid item reference '" + namespacedId + "'. Expected format 'namespace:item-id'");
            return Optional.empty();
        }

        String namespace = namespacedId.substring(0, colon);
        String itemId    = namespacedId.substring(colon + 1);

        ItemProvider provider = providers.get(namespace);
        if (provider == null) {
            warn("No provider registered for namespace '" + namespace + "' (referenced by '" + namespacedId + "')");
            return Optional.empty();
        }

        Optional<ItemStack> result = provider.provide(itemId);
        if (result.isEmpty()) {
            warn("Provider '" + namespace + "' does not recognise item ID '" + itemId + "'");
        }
        return result;
    }

    /**
     * Returns whether the given namespaced ID resolves to a known item.
     * Useful for config validation at load time.
     *
     * @param namespacedId the item reference to check
     * @return true if the item can be built
     */
    public static boolean exists(String namespacedId) {
        int colon = namespacedId.indexOf(':');
        if (colon == -1) return false;
        ItemProvider provider = providers.get(namespacedId.substring(0, colon));
        if (provider == null) return false;
        return provider.itemIds().contains(namespacedId.substring(colon + 1));
    }

    /**
     * Returns all item IDs registered under the given namespace.
     * Returns an empty set if the namespace is not registered.
     *
     * @param namespace the namespace to query
     * @return the registered item IDs
     */
    public static Set<String> itemIds(String namespace) {
        ItemProvider provider = providers.get(namespace);
        return provider != null ? provider.itemIds() : Set.of();
    }

    /**
     * Returns every namespaced item ID across all registered providers,
     * in the form {@code "namespace:item-id"}.
     * Useful for tab completion and admin tooling.
     *
     * @return the full set of registered namespaced item IDs
     */
    public static Set<String> allNamespacedIds() {
        Set<String> result = new java.util.LinkedHashSet<>();
        for (java.util.Map.Entry<String, ItemProvider> entry : providers.entrySet()) {
            for (String id : entry.getValue().itemIds()) {
                result.add(entry.getKey() + ":" + id);
            }
        }
        return result;
    }

    private static void warn(String message) {
        if (logger != null) logger.warning("[ItemRegistry] " + message);
    }
}