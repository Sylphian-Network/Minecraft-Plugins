package net.sylphian.minecraft.entities.entity;

import net.sylphian.minecraft.entities.config.EntitiesConfig;
import net.sylphian.minecraft.entities.config.EntityDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Optional;
import java.util.Set;

/**
 * The built-in entity provider, exposing every entity defined in the
 * Sylphian-Entities {@code config.yml} under the {@code sylphian-entities}
 * namespace. Reload swaps the backing config by reference.
 */
public final class ConfiguredEntityProvider implements EntityProvider {

    /** The namespace all config-defined entities are registered under. */
    public static final String NAMESPACE = "sylphian-entities";

    private volatile EntitiesConfig config;

    public ConfiguredEntityProvider(EntitiesConfig config) {
        this.config = config;
    }

    /**
     * Swaps in a freshly parsed configuration.
     *
     * @param newConfig the new config holder
     */
    public void reload(EntitiesConfig newConfig) {
        this.config = newConfig;
    }

    @Override
    public String namespace() {
        return NAMESPACE;
    }

    @Override
    public Optional<Entity> spawn(String entityId, Location location) {
        EntityDefinition definition = config.entities().get(entityId);
        if (definition == null) return Optional.empty();
        return Optional.of(definition.builder().spawn(location));
    }

    @Override
    public Set<String> entityIds() {
        return config.entities().keySet();
    }
}
