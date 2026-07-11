package net.sylphian.minecraft.dimensions.spawn;

import java.util.List;

/**
 * A dimension's spawn configuration: the global caps and range that shape a
 * vanilla-like spawn cycle, plus the weighted table of entries to draw from.
 *
 * @param enabled          whether custom spawning runs in this dimension
 * @param mobCap           base cap, scaled by the count of player-loaded chunks (vanilla uses 289 as the reference chunk count)
 * @param perChunkCap      the maximum tracked spawns allowed in a single chunk
 * @param spawnRangeChunks the chunk radius around each player that is eligible for spawning
 * @param entries          the weighted spawn table
 */
public record SpawnSettings(
        boolean enabled,
        int mobCap,
        int perChunkCap,
        int spawnRangeChunks,
        List<SpawnEntry> entries) {

    /** The disabled state used when a dimension declares no {@code spawns} block. */
    public static final SpawnSettings DISABLED = new SpawnSettings(false, 0, 0, 0, List.of());
}
