package net.sylphian.minecraft.fishing.services;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.config.BaitConfig;
import net.sylphian.minecraft.fishing.services.bait.BaitZone;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;

/**
 * Manages bait configurations and active bait zones.
 *
 * <p>Owns the loaded bait config map (mirroring how {@link LootService} owns
 * the fish pool) so no other class needs to hold or reload it separately.</p>
 */
public class BaitZoneService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Map<String, BaitConfig> baits;
    private final List<BaitZone> activeZones = new ArrayList<>();
    private BukkitTask renderTask;
    private BukkitTask labelTask;

    /**
     * Constructs a new BaitZoneService.
     *
     * @param baits the initial map of bait configurations loaded from baits.yml
     */
    public BaitZoneService(Map<String, BaitConfig> baits) {
        this.baits = baits;
    }

    /**
     * Starts the rendering and label update tasks.
     *
     * @param plugin the plugin instance for scheduling
     */
    public void start(JavaPlugin plugin) {
        renderTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::renderAndExpire, 0L, 4L);
        labelTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateLabels, 0L, 20L);
    }

    /**
     * Cancels all tasks and removes all active zones and their display entities.
     */
    public void shutdown() {
        if (renderTask != null) renderTask.cancel();
        if (labelTask != null) labelTask.cancel();
        activeZones.forEach(BaitZone::removeDisplay);
        activeZones.clear();
    }

    /**
     * Reloads the bait configuration map. Active zones are unaffected —
     * they continue with the config they were created with.
     *
     * @param baits the updated bait configurations
     */
    public void reload(Map<String, BaitConfig> baits) {
        this.baits = baits;
    }

    /**
     * Returns the config for the given bait ID, or null if not found.
     *
     * @param id the bait identifier
     * @return the BaitConfig, or null
     */
    @Nullable
    public BaitConfig getBaitConfig(String id) {
        return baits.get(id);
    }

    /**
     * Creates a new bait zone centred at the given location.
     *
     * @param centre the water surface location the bait landed at
     * @param config the bait configuration for this zone
     */
    public void createZone(Location centre, BaitConfig config) {
        for (BaitZone zone : activeZones) {
            if (zone.config().id().equals(config.id()) && zone.contains(centre)) {
                zone.extendExpiry(config.durationSeconds());
                zone.updateLabel(buildLabel(zone.config().displayName(), (int) zone.secondsRemaining()));
                return;
            }
        }

        Instant expiry = Instant.now().plusSeconds(config.durationSeconds());

        Location displayLoc = centre.clone().add(0, 1.8, 0);
        TextDisplay display = centre.getWorld().spawn(displayLoc, TextDisplay.class, entity -> {
            entity.text(buildLabel(config.displayName(), config.durationSeconds()));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            entity.setPersistent(false);
        });

        activeZones.add(new BaitZone(centre, config, expiry, display));
    }

    /**
     * Returns at most one active zone per bait type whose radius contains the given location.
     * If multiple zones of the same type overlap, only the first (oldest) is included.
     *
     * @param location the location to check
     * @return a list of matching BaitZones with no duplicate bait types, empty if none
     */
    public List<BaitZone> getZonesAt(Location location) {
        Map<String, BaitZone> byType = new LinkedHashMap<>();
        for (BaitZone zone : activeZones) {
            if (zone.contains(location)) {
                byType.putIfAbsent(zone.config().id(), zone);
            }
        }
        return List.copyOf(byType.values());
    }

    /**
     * Returns the combined bite timer multiplier for all active zones at the given location.
     * Each zone's multiplier is multiplied together. Returns {@code 1.0} if no zones are active.
     *
     * @param location the location to check
     * @return the combined multiplier to apply to the bite wait time
     */
    public double getBiteTimerMultiplier(Location location) {
        return getZonesAt(location).stream()
                .mapToDouble(z -> z.config().biteTimerMultiplier())
                .reduce(1.0, (a, b) -> a * b);
    }

    private void renderAndExpire() {
        Iterator<BaitZone> it = activeZones.iterator();
        while (it.hasNext()) {
            BaitZone zone = it.next();
            if (zone.isExpired()) {
                zone.removeDisplay();
                it.remove();
                continue;
            }
            drawRing(zone);
        }
    }

    private void drawRing(BaitZone zone) {
        Location centre = zone.centre();
        if (centre.getWorld() == null) return;

        Particle particle;
        try {
            particle = Particle.valueOf(zone.config().particle().toUpperCase());
        } catch (IllegalArgumentException e) {
            particle = Particle.BUBBLE;
        }

        double radius = zone.config().radius();
        double circumference = 2 * Math.PI * radius;
        int points = (int) Math.ceil(circumference / 0.4);
        double angleStep = (2 * Math.PI) / points;

        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            double x = centre.getX() + radius * Math.cos(angle);
            double z = centre.getZ() + radius * Math.sin(angle);
            centre.getWorld().spawnParticle(particle, x, centre.getY() + 0.1, z, 1, 0, 0, 0, 0);
        }
    }

    private void updateLabels() {
        for (BaitZone zone : activeZones) {
            zone.updateLabel(buildLabel(zone.config().displayName(), (int) zone.secondsRemaining()));
        }
    }

    private Component buildLabel(String displayName, int secondsRemaining) {
        int mins = secondsRemaining / 60;
        int secs = secondsRemaining % 60;
        String time = mins > 0 ? mins + "m " + secs + "s" : secs + "s";
        return MINI.deserialize(displayName + "\n<gray>" + time);
    }

    /**
     * Returns all registered bait IDs.
     *
     * @return the collection of bait identifiers currently loaded from baits.yml
     */
    public Collection<String> getBaitIds() {
        return baits.keySet();
    }
}