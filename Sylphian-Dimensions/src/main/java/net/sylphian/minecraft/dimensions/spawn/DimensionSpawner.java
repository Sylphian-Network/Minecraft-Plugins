package net.sylphian.minecraft.dimensions.spawn;

import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.world.DimensionManager;
import net.sylphian.minecraft.entities.entity.EntityRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Drives vanilla-like custom mob spawning for every dimension that defines a
 * spawn table. Each cycle it gathers the chunks players have loaded, scales a
 * global cap by that count, and pack-spawns weighted entries on valid ground,
 * honouring per-chunk caps, a no-spawn radius around players, and each entry's
 * time, light, and Y conditions.
 *
 * <p>Cost scales with activity: dimensions with no players are skipped
 * entirely. This is the only class that references {@link EntityRegistry}, so
 * it is instantiated only when Sylphian-Entities is present.</p>
 */
public final class DimensionSpawner {

    // Vanilla scales the mob cap against a 17x17 chunk reference area
    private static final double REFERENCE_CHUNKS = 289.0;
    // Mobs never spawn within this block distance of a player
    private static final double NO_SPAWN_RADIUS_SQUARED = 24.0 * 24.0;
    // Cap chunk attempts per dimension per cycle to bound cost on dense areas
    private static final int MAX_ATTEMPTS_PER_CYCLE = 40;
    // Horizontal spread of pack members around the anchor
    private static final int PACK_SPREAD = 4;
    // Vertical window a pack member may sit above or below the anchor's level
    private static final int PACK_VERTICAL_WINDOW = 5;
    // Sentinel returned when a column has no valid standing spot
    private static final int NO_SPOT = Integer.MIN_VALUE;
    // A single column scan never covers more than this many Y levels; a wider
    // configured band is sampled in a random sub-window each attempt, so
    // main-thread cost stays bounded no matter how wide the config is
    private static final int MAX_SCAN_BAND = 128;

    private final Plugin plugin;
    private final DimensionManager manager;
    private final Random random = new Random();
    // Dimensions already warned about a wide scan band; cleared on reload
    private final Set<String> warnedWideScanBands = new HashSet<>();

    private int taskId = -1;

    public DimensionSpawner(Plugin plugin, DimensionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /** Schedules the repeating spawn cycle at the configured interval. */
    public void start() {
        int interval = manager.spawnIntervalTicks();
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval).getTaskId();
    }

    /** Cancels the repeating spawn cycle. */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /** Reschedules the cycle, picking up an interval change from a reload. */
    public void reload() {
        stop();
        warnedWideScanBands.clear();
        start();
    }

    private void tick() {
        for (Dimension dimension : manager.dimensions()) {
            SpawnSettings settings = dimension.spawnSettings();
            if (!settings.enabled()) continue;

            World world = Bukkit.getWorld(DimensionManager.worldKey(dimension.name()));
            if (world == null) continue;

            List<Player> players = world.getPlayers();
            if (players.isEmpty()) continue;

            spawnCycle(world, dimension.name(), settings, players);
        }
    }

    private void spawnCycle(World world, String dimensionName, SpawnSettings settings, List<Player> players) {
        Set<Long> eligibleChunks = eligibleChunks(world, settings.spawnRangeChunks(), players);
        if (eligibleChunks.isEmpty()) return;

        int cap = (int) Math.ceil(settings.mobCap() * (eligibleChunks.size() / REFERENCE_CHUNKS));
        if (cap <= 0) return;

        Map<Long, Integer> perChunk = new HashMap<>();
        int current = countSpawned(world, perChunk);
        int budget = cap - current;
        if (budget <= 0) return;

        // The band the column scan covers: the union of every entry's Y range,
        // clamped to the world, so a spot is only scanned if some entry wants it
        int scanMin = Math.max(world.getMinHeight() + 1, minEntryY(settings));
        int scanMax = Math.min(world.getMaxHeight() - 1, maxEntryY(settings));
        if (scanMin > scanMax) return;
        warnWideScanBand(dimensionName, scanMin, scanMax);

        List<Long> chunks = new ArrayList<>(eligibleChunks);
        Collections.shuffle(chunks, random);

        int attempts = 0;
        for (long chunkKey : chunks) {
            if (budget <= 0 || attempts >= MAX_ATTEMPTS_PER_CYCLE) break;

            // attempts++ sits below this skip on purpose: a saturated chunk must
            // not burn one of the limited per-cycle attempts and starve others
            if (perChunk.getOrDefault(chunkKey, 0) >= settings.perChunkCap()) continue;
            attempts++;
            budget -= attemptPack(world, dimensionName, settings, players, chunkKey, perChunk, scanMin, scanMax, budget);
        }
    }

    // Warns once per dimension when its band exceeds the per-scan cap, so it is
    // sampled in windows. Informational: cost stays bounded either way. Throttled
    // via warnedWideScanBands so it fires once, not every cycle.
    private void warnWideScanBand(String dimensionName, int scanMin, int scanMax) {
        int bandSize = scanMax - scanMin + 1;
        if (bandSize <= MAX_SCAN_BAND) return;
        if (warnedWideScanBands.add(dimensionName)) {
            plugin.getLogger().warning("Dimension '" + dimensionName + "' spawn band spans " + bandSize
                    + " Y levels (" + scanMin + ".." + scanMax + "), wider than the " + MAX_SCAN_BAND
                    + "-level scan cap; it is sampled in random windows each cycle. Narrow the spawn "
                    + "entries' min-y/max-y for denser, more predictable spawns.");
        }
    }

    private int minEntryY(SpawnSettings settings) {
        int min = Integer.MAX_VALUE;
        for (SpawnEntry entry : settings.entries()) min = Math.min(min, entry.minY());
        return min;
    }

    private int maxEntryY(SpawnSettings settings) {
        int max = Integer.MIN_VALUE;
        for (SpawnEntry entry : settings.entries()) max = Math.max(max, entry.maxY());
        return max;
    }

    private Set<Long> eligibleChunks(World world, int range, List<Player> players) {
        Set<Long> chunks = new HashSet<>();
        for (Player player : players) {
            int pcx = player.getLocation().getBlockX() >> 4;
            int pcz = player.getLocation().getBlockZ() >> 4;
            for (int dx = -range; dx <= range; dx++) {
                for (int dz = -range; dz <= range; dz++) {
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    if (world.isChunkLoaded(cx, cz)) chunks.add(chunkKey(cx, cz));
                }
            }
        }
        return chunks;
    }

    private int countSpawned(World world, Map<Long, Integer> perChunk) {
        int count = 0;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (!entity.getPersistentDataContainer().has(SpawnTags.SOURCE, PersistentDataType.STRING)) continue;
            count++;
            Location loc = entity.getLocation();
            perChunk.merge(chunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4), 1, Integer::sum);
        }
        return count;
    }

    private int attemptPack(World world, String dimensionName, SpawnSettings settings, List<Player> players,
                            long chunkKey, Map<Long, Integer> perChunk, int scanMin, int scanMax, int budget) {
        int cx = (int) (chunkKey & 0xffffffffL);
        int cz = (int) (chunkKey >> 32);
        int anchorX = (cx << 4) + random.nextInt(16);
        int anchorZ = (cz << 4) + random.nextInt(16);

        // Bound the scan to at most MAX_SCAN_BAND levels per attempt. A wider
        // band is sampled by a random window, so cost is capped while every
        // level stays reachable across cycles.
        int windowMin = scanMin;
        if (scanMax - scanMin + 1 > MAX_SCAN_BAND) {
            windowMin = scanMin + random.nextInt(scanMax - scanMin - MAX_SCAN_BAND + 2);
        }
        int windowMax = Math.min(scanMax, windowMin + MAX_SCAN_BAND - 1);

        int anchorY = randomStandingY(world, anchorX, anchorZ, windowMin, windowMax);
        if (anchorY == NO_SPOT || tooCloseToPlayer(players, anchorX, anchorY, anchorZ)) return 0;

        // Block (artificial) light only: sky light is not time-adjusted here, so
        // day/night is handled by the entry's time window, not this value
        int light = world.getBlockAt(anchorX, anchorY, anchorZ).getLightFromBlocks();
        SpawnEntry entry = pickEntry(settings.entries(), world, anchorY, light);
        if (entry == null) return 0;

        int groupSize = entry.minGroup() + random.nextInt(entry.maxGroup() - entry.minGroup() + 1);
        int spawned = 0;
        for (int i = 0; i < groupSize && spawned < budget; i++) {
            int mx = anchorX + random.nextInt(PACK_SPREAD * 2 + 1) - PACK_SPREAD;
            int mz = anchorZ + random.nextInt(PACK_SPREAD * 2 + 1) - PACK_SPREAD;
            // Spread can push a member into a neighbouring chunk, so cap-check and
            // count under the member's own chunk, not the anchor's
            long memberChunk = chunkKey(mx >> 4, mz >> 4);
            if (perChunk.getOrDefault(memberChunk, 0) >= settings.perChunkCap()) continue;

            int my = nearestStandingY(world, mx, mz, anchorY, PACK_VERTICAL_WINDOW);
            if (my == NO_SPOT || tooCloseToPlayer(players, mx, my, mz)) continue;

            // Light and Y are validated at the anchor only, but the member sits at a
            // different spot, so re-check the entry's conditions where it will land
            int memberLight = world.getBlockAt(mx, my, mz).getLightFromBlocks();
            if (!entry.matches(world, my, memberLight)) continue;

            Location loc = new Location(world, mx + 0.5, my, mz + 0.5, random.nextFloat() * 360f, 0f);
            if (spawnTagged(entry.entityId(), loc, dimensionName)) {
                spawned++;
                perChunk.merge(memberChunk, 1, Integer::sum);
            }
        }
        return spawned;
    }

    // Random spot across the band rather than just the surface, so mobs can
    // spawn on any level: cave floors, under canopies, or the surface
    private int randomStandingY(World world, int x, int z, int minY, int maxY) {
        List<Integer> spots = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            if (isSpawnable(world, x, y, z)) spots.add(y);
        }
        return spots.isEmpty() ? NO_SPOT : spots.get(random.nextInt(spots.size()));
    }

    // Closest spot to the anchor level, so pack members stay together rather
    // than scattering across the column
    private int nearestStandingY(World world, int x, int z, int anchorY, int window) {
        for (int d = 0; d <= window; d++) {
            if (isSpawnable(world, x, anchorY + d, z)) return anchorY + d;
            if (d > 0 && isSpawnable(world, x, anchorY - d, z)) return anchorY - d;
        }
        return NO_SPOT;
    }

    private boolean spawnTagged(String entityId, Location location, String dimensionName) {
        Entity entity = EntityRegistry.spawn(entityId, location).orElse(null);
        if (entity == null) return false;
        entity.getPersistentDataContainer().set(SpawnTags.SOURCE, PersistentDataType.STRING, dimensionName);
        return true;
    }

    // A valid standing spot: solid non-leaf floor, and dry passable feet and head space
    private boolean isSpawnable(World world, int x, int y, int z) {
        if (y <= world.getMinHeight() + 1 || y >= world.getMaxHeight()) return false;
        Block floor = world.getBlockAt(x, y - 1, z);
        if (!floor.getType().isSolid() || Tag.LEAVES.isTagged(floor.getType())) return false;
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        return feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid();
    }

    private boolean tooCloseToPlayer(List<Player> players, int x, int y, int z) {
        for (Player player : players) {
            if (player.getLocation().distanceSquared(new Location(player.getWorld(), x, y, z)) < NO_SPAWN_RADIUS_SQUARED) {
                return true;
            }
        }
        return false;
    }

    private SpawnEntry pickEntry(List<SpawnEntry> entries, World world, int y, int light) {
        int totalWeight = 0;
        List<SpawnEntry> eligible = new ArrayList<>();
        for (SpawnEntry entry : entries) {
            if (entry.matches(world, y, light)) {
                eligible.add(entry);
                totalWeight += entry.weight();
            }
        }
        if (eligible.isEmpty()) return null;

        int roll = random.nextInt(totalWeight);
        for (SpawnEntry entry : eligible) {
            roll -= entry.weight();
            if (roll < 0) return entry;
        }
        return eligible.getLast();
    }

    private static long chunkKey(int x, int z) {
        return (x & 0xffffffffL) | ((long) z << 32);
    }
}
