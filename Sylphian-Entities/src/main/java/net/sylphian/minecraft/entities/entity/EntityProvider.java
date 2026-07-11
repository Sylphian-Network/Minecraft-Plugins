package net.sylphian.minecraft.entities.entity;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import java.util.Optional;
import java.util.Set;

/**
 * Implemented by any plugin that wants to expose custom entities to other plugins.
 * Register an implementation with {@link EntityRegistry} on enable and unregister on disable.
 */
public interface EntityProvider {

    /**
     * Returns the namespace for this provider, e.g. {@code "sylphian-entities"}.
     * Must be unique across all registered providers.
     *
     * @return the namespace
     */
    String namespace();

    /**
     * Spawns the entity for the given entity ID at the given location.
     * Each call spawns a fresh instance.
     *
     * @param entityId the entity identifier, e.g. {@code "rift_wolf"}
     * @param location the location to spawn at
     * @return the spawned entity, or empty if the ID is not recognised
     */
    Optional<Entity> spawn(String entityId, Location location);

    /**
     * Returns all entity IDs this provider can spawn.
     * Used for config validation and tab completion.
     *
     * @return the set of registered entity IDs
     */
    Set<String> entityIds();
}
