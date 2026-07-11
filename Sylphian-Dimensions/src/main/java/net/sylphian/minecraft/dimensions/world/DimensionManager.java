package net.sylphian.minecraft.dimensions.world;

import net.sylphian.minecraft.dimensions.api.DimensionAPI;
import net.sylphian.minecraft.dimensions.config.DimensionsConfig;
import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.dimensions.model.DimensionRuleset;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Owns dimension world lifecycle and entry, and implements the public API.
 * Worlds are created under the {@code sylphian} namespace, so their data lives
 * at {@code world/dimensions/sylphian/<name>/}. All entry goes through
 * {@link #enter(Player, String)}.
 */
public class DimensionManager implements DimensionAPI {

    /** The namespace all dimension worlds are keyed under. */
    public static final String NAMESPACE = "sylphian";

    private final TemplateManager templates;
    private final Logger logger;

    // volatile + concurrent: read from the async spawn-location event during the configuration phase
    private volatile DimensionsConfig config;
    private final Map<NamespacedKey, Dimension> byWorldKey = new ConcurrentHashMap<>();

    /**
     * @param config    the parsed configuration
     * @param templates the template manager preparing dimension folders
     * @param logger    the logger for progress and warnings
     */
    public DimensionManager(DimensionsConfig config, TemplateManager templates, Logger logger) {
        this.config = config;
        this.templates = templates;
        this.logger = logger;
    }

    /**
     * Returns the world key backing the given dimension, e.g. {@code sylphian:hub}.
     *
     * @param dimensionName the dimension name from config
     * @return the world key
     */
    public static NamespacedKey worldKey(String dimensionName) {
        return new NamespacedKey(NAMESPACE, dimensionName);
    }

    /**
     * Prepares and loads every configured dimension world and applies its border.
     * A single failed dimension is logged and skipped, never aborting the rest.
     *
     * @return true if the hub dimension loaded successfully
     */
    public boolean loadAll() {
        boolean hubLoaded = false;
        for (Dimension dimension : config.dimensions().values()) {
            try {
                templates.prepare(dimension);
            } catch (IOException e) {
                logger.severe("Failed to prepare dimension '" + dimension.name() + "': " + e);
                continue;
            }

            if (loadDimension(dimension) && dimension.name().equals(config.hubName())) {
                hubLoaded = true;
            }
        }
        return hubLoaded;
    }

    /**
     * Loads a single dimension's world and applies its border.
     * The active folder must already be prepared.
     *
     * @param dimension the dimension to load
     * @return true if the world loaded
     */
    public boolean loadDimension(Dimension dimension) {
        World world = WorldCreator.ofKey(worldKey(dimension.name()))
                .generator(new VoidChunkGenerator())
                .createWorld();
        if (world == null) {
            logger.severe("Failed to load world for dimension '" + dimension.name() + "'.");
            return false;
        }

        applyBorder(world, dimension);
        byWorldKey.put(world.getKey(), dimension);
        logger.info("Dimension '" + dimension.name() + "' active.");
        return true;
    }

    /**
     * Unloads a dimension's world without saving, moving players inside to the
     * hub first (or the default world spawn when unloading the hub itself).
     * Must be called on the main thread.
     *
     * @param name the dimension name
     * @return true if the world was unloaded
     */
    public boolean unloadForMigration(String name) {
        World world = Bukkit.getWorld(worldKey(name));
        if (world == null) return false;

        Location evacuation = name.equals(config.hubName()) ? null : hubSpawn();
        if (evacuation == null) evacuation = Bukkit.getWorlds().getFirst().getSpawnLocation();

        for (Player player : world.getPlayers()) {
            player.teleport(evacuation);
        }

        if (!Bukkit.unloadWorld(world, false)) return false;
        byWorldKey.remove(worldKey(name));
        return true;
    }

    /**
     * Teleports a player into a dimension's spawn point.
     *
     * @param player the player to move
     * @param name   the dimension name
     * @return true if the dimension exists and its world is loaded
     */
    public boolean enter(Player player, String name) {
        Dimension dimension = config.dimensions().get(name);
        if (dimension == null) return false;
        Location spawn = spawnLocation(dimension);
        if (spawn == null) return false;
        player.teleport(spawn);
        return true;
    }

    /**
     * Teleports a player to the hub dimension's spawn point.
     *
     * @param player the player to move
     * @return true if the hub world is loaded
     */
    public boolean toHub(Player player) {
        return enter(player, config.hubName());
    }

    /**
     * Returns the hub dimension's spawn location.
     *
     * @return the hub spawn, or null if the hub world is not loaded
     */
    public @Nullable Location hubSpawn() {
        Dimension hub = config.dimensions().get(config.hubName());
        return hub != null ? spawnLocation(hub) : null;
    }

    /**
     * Returns the live spawn location inside a dimension's world.
     *
     * @param dimension the dimension to resolve
     * @return the spawn location, or null if the world is not loaded
     */
    public @Nullable Location spawnLocation(Dimension dimension) {
        World world = Bukkit.getWorld(worldKey(dimension.name()));
        if (world == null) return null;
        Dimension.SpawnPoint sp = dimension.spawnPoint();
        return new Location(world, sp.x(), sp.y(), sp.z(), sp.yaw(), sp.pitch());
    }

    /**
     * Swaps in a new configuration. Rulesets refresh immediately for loaded
     * worlds; adding or removing dimensions requires a restart and is logged.
     *
     * @param newConfig the freshly parsed configuration
     */
    public void reload(DimensionsConfig newConfig) {
        this.config = newConfig;
        byWorldKey.clear();
        for (Dimension dimension : newConfig.dimensions().values()) {
            NamespacedKey key = worldKey(dimension.name());
            if (Bukkit.getWorld(key) != null) {
                byWorldKey.put(key, dimension);
            } else {
                logger.warning("Dimension '" + dimension.name() + "' was added while running; restart required to load its world.");
            }
        }
    }

    /**
     * Saves and unloads every managed world, moving any players inside to the
     * default world spawn first. Call from onDisable.
     */
    public void unloadAll() {
        Location fallback = Bukkit.getWorlds().getFirst().getSpawnLocation();
        for (NamespacedKey key : byWorldKey.keySet()) {
            World world = Bukkit.getWorld(key);
            if (world == null) continue;
            for (Player player : world.getPlayers()) {
                player.teleport(fallback);
            }
            Bukkit.unloadWorld(world, true);
        }
        byWorldKey.clear();
    }

    @Override
    public Optional<Dimension> getDimension(String name) {
        return Optional.ofNullable(config.dimensions().get(name));
    }

    @Override
    public Optional<Dimension> getDimensionByWorld(World world) {
        return Optional.ofNullable(byWorldKey.get(world.getKey()));
    }

    @Override
    public Optional<Dimension> getPlayerCurrentDimension(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return Optional.empty();
        return getDimensionByWorld(player.getWorld());
    }

    @Override
    public DimensionRuleset getRuleset(Dimension dimension) {
        return dimension.ruleset();
    }

    @Override
    public Set<String> dimensionNames() {
        return config.dimensions().keySet();
    }

    /**
     * Returns the name of the dimension configured as the hub.
     *
     * @return the hub dimension name
     */
    public String hubName() {
        return config.hubName();
    }

    /**
     * Returns every configured dimension. Reflects the current config, so a
     * caller iterating each tick always sees the latest reloaded definitions.
     *
     * @return the configured dimensions
     */
    public Collection<Dimension> dimensions() {
        return config.dimensions().values();
    }

    /**
     * Returns the tick interval between custom-spawner cycles.
     *
     * @return the spawn interval in ticks
     */
    public int spawnIntervalTicks() {
        return config.spawnIntervalTicks();
    }

    // Templates are built centered on world 0,0; the border matches the built area
    private void applyBorder(World world, Dimension dimension) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(Math.max(dimension.chunkBoundsX(), dimension.chunkBoundsZ()) * 16.0);
    }
}
