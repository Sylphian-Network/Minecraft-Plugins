package net.sylphian.minecraft.dimensions.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.jspecify.annotations.NonNull;

import java.util.Random;

/**
 * Generates nothing: all terrain comes from the pre-built template chunks,
 * so nothing outside the built area ever generates.
 */
public final class VoidChunkGenerator extends ChunkGenerator {

    /** Fixes the spawn search so Bukkit never scans void chunks for solid ground. */
    @Override
    public Location getFixedSpawnLocation(@NonNull World world, @NonNull Random random) {
        return new Location(world, 0.5, 65.0, 0.5);
    }
}
