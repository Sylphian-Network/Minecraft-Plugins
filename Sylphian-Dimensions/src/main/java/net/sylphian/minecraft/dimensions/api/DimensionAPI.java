package net.sylphian.minecraft.dimensions.api;

import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.model.DimensionRuleset;
import org.bukkit.World;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Read access to managed dimensions for other plugins.
 *
 * <p>An empty result means "not a dimension": the player or world is in the
 * overworld (or another unmanaged world) and vanilla rules apply.</p>
 */
public interface DimensionAPI {

    /**
     * Returns the dimension with the given config name.
     *
     * @param name the dimension name, e.g. {@code "rift-gathering"}
     * @return the dimension, or empty if not defined
     */
    Optional<Dimension> getDimension(String name);

    /**
     * Returns the dimension backing the given Bukkit world.
     * Resolved by world key, e.g. {@code sylphian:hub}.
     *
     * @param world the Bukkit world
     * @return the dimension, or empty if the world is not managed
     */
    Optional<Dimension> getDimensionByWorld(World world);

    /**
     * Returns the dimension the player is currently inside.
     *
     * @param uuid the player's Mojang UUID
     * @return the dimension, or empty if the player is offline or in an unmanaged world
     */
    Optional<Dimension> getPlayerCurrentDimension(UUID uuid);

    /**
     * Returns the resolved ruleset for a dimension.
     *
     * @param dimension the dimension to query
     * @return the ruleset (type defaults plus config overrides)
     */
    DimensionRuleset getRuleset(Dimension dimension);

    /**
     * Returns the names of all defined dimensions.
     * Used for config validation and tab completion.
     *
     * @return the dimension names
     */
    Set<String> dimensionNames();
}
