package net.sylphian.minecraft.entities.entity;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for cross-plugin custom entities.
 *
 * <p>Plugins register an {@link EntityProvider} on enable and unregister on disable.
 * Entities are referenced by a namespaced ID in the form {@code namespace:entity-id},
 * e.g. {@code "sylphian-entities:rift_wolf"}.</p>
 *
 * <p>The registry is thread-safe. Entities are spawned only when requested.</p>
 */
public final class EntityRegistry {

    private static final ConcurrentHashMap<String, EntityProvider> providers = new ConcurrentHashMap<>();
    private static Logger logger;

    private EntityRegistry() {}

    /**
     * Sets the logger used for warnings. Call once from Sylphian-Entities onEnable.
     *
     * @param log the logger to use
     */
    public static void init(Logger log) {
        logger = log;
    }

    /**
     * Clears the retained logger. Call from Sylphian-Entities onDisable so a
     * disabled plugin's logger is not held.
     */
    public static void shutdown() {
        logger = null;
    }

    /**
     * Registers an entity provider under its declared namespace.
     * Replaces any existing provider with the same namespace.
     *
     * @param provider the provider to register
     */
    public static void register(EntityProvider provider) {
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
     * Spawns the entity for the given namespaced ID at the given location.
     * Must be called on the main thread.
     *
     * @param namespacedId the entity reference in the form {@code "namespace:entity-id"}
     * @param location     the location to spawn at
     * @return the spawned entity, or empty if the namespace or entity ID is not found
     */
    public static Optional<Entity> spawn(String namespacedId, Location location) {
        int colon = namespacedId.indexOf(':');
        if (colon == -1) {
            warn("Invalid entity reference '" + namespacedId + "'. Expected format 'namespace:entity-id'");
            return Optional.empty();
        }

        String namespace = namespacedId.substring(0, colon);
        String entityId  = namespacedId.substring(colon + 1);

        EntityProvider provider = providers.get(namespace);
        if (provider == null) {
            warn("No provider registered for namespace '" + namespace + "' (referenced by '" + namespacedId + "')");
            return Optional.empty();
        }

        Optional<Entity> result = provider.spawn(entityId, location);
        if (result.isEmpty()) {
            warn("Provider '" + namespace + "' does not recognise entity ID '" + entityId + "'");
        }
        return result;
    }

    /**
     * Returns whether the given namespaced ID resolves to a known entity.
     * Useful for config validation at load time.
     *
     * @param namespacedId the entity reference to check
     * @return true if the entity can be spawned
     */
    public static boolean exists(String namespacedId) {
        int colon = namespacedId.indexOf(':');
        if (colon == -1) return false;
        EntityProvider provider = providers.get(namespacedId.substring(0, colon));
        if (provider == null) return false;
        return provider.entityIds().contains(namespacedId.substring(colon + 1));
    }

    /**
     * Returns all entity IDs registered under the given namespace.
     * Returns an empty set if the namespace is not registered.
     *
     * @param namespace the namespace to query
     * @return the registered entity IDs
     */
    public static Set<String> entityIds(String namespace) {
        EntityProvider provider = providers.get(namespace);
        return provider != null ? provider.entityIds() : Set.of();
    }

    /**
     * Returns every namespaced entity ID across all registered providers,
     * in the form {@code "namespace:entity-id"}.
     * Useful for tab completion and admin tooling.
     *
     * @return the full set of registered namespaced entity IDs
     */
    public static Set<String> allNamespacedIds() {
        Set<String> result = new java.util.LinkedHashSet<>();
        for (Entry<String, EntityProvider> entry : providers.entrySet()) {
            for (String id : entry.getValue().entityIds()) {
                result.add(entry.getKey() + ":" + id);
            }
        }
        return result;
    }

    private static void warn(String message) {
        if (logger != null) logger.warning("[EntityRegistry] " + message);
    }
}
