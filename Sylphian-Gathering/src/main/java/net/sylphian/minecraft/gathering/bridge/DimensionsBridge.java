package net.sylphian.minecraft.gathering.bridge;

import net.sylphian.minecraft.dimensions.api.DimensionProvider;
import net.sylphian.minecraft.dimensions.model.Dimension;
import org.bukkit.World;

import java.util.Optional;

/**
 * Isolates every reference to Sylphian-Dimensions so the engine reads dimension
 * rulesets through one place. Returns empty results when Dimensions is absent.
 */
public final class DimensionsBridge {

    private DimensionsBridge() {}

    /**
     * Returns the dimension backing the given world.
     *
     * @param world the Bukkit world
     * @return the dimension, or empty if the world is unmanaged
     */
    public static Optional<Dimension> getDimensionByWorld(World world) {
        return DimensionProvider.isAvailable() ? DimensionProvider.get().getDimensionByWorld(world) : Optional.empty();
    }

    /**
     * Returns the dimension name backing the given world.
     *
     * @param world the Bukkit world
     * @return the dimension name, or empty if the world is unmanaged or Dimensions is absent
     */
    public static Optional<String> getDimensionName(World world) {
        return getDimensionByWorld(world).map(Dimension::name);
    }
}
