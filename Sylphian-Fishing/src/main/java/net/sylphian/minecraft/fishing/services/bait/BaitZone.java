package net.sylphian.minecraft.fishing.services.bait;

import net.kyori.adventure.text.Component;
import net.sylphian.minecraft.fishing.config.BaitConfig;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;

import java.time.Instant;

/**
 * Represents an active bait zone in the world.
 *
 * <p>Zones are created when a bait item lands in water and persist
 * until their duration expires. Any fishing hook that lands within
 * the zone's radius receives the configured bonuses.</p>
 */
public class BaitZone {

    private final Location centre;
    private final BaitConfig config;
    private final Instant expiry;
    private final TextDisplay display;

    /**
     * Constructs a new BaitZone.
     *
     * @param centre  the centre of the zone at water surface level
     * @param config  the bait configuration driving this zone
     * @param expiry  the instant this zone expires
     * @param display the text display entity above the centre
     */
    public BaitZone(Location centre, BaitConfig config, Instant expiry, TextDisplay display) {
        this.centre = centre;
        this.config = config;
        this.expiry = expiry;
        this.display = display;
    }

    public Location centre() { return centre; }
    public BaitConfig config() { return config; }

    /**
     * Returns true if this zone has passed its expiry time.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiry);
    }

    /**
     * Returns the number of seconds remaining before this zone expires.
     *
     * @return seconds remaining, clamped to zero
     */
    public long secondsRemaining() {
        return Math.max(0, expiry.getEpochSecond() - Instant.now().getEpochSecond());
    }

    /**
     * Returns true if the given location falls within this zone's radius.
     * Uses a flat 2D distance check — Y coordinate is ignored.
     *
     * @param location the location to test
     * @return true if inside the zone
     */
    public boolean contains(Location location) {
        if (!location.getWorld().equals(centre.getWorld())) return false;
        double dx = location.getX() - centre.getX();
        double dz = location.getZ() - centre.getZ();
        return Math.sqrt(dx * dx + dz * dz) <= config.radius();
    }

    /**
     * Updates the text shown on the floating display entity.
     *
     * @param text the new display text
     */
    public void updateLabel(Component text) {
        if (!display.isDead()) display.text(text);
    }

    /**
     * Removes the text display entity from the world if it is still alive.
     */
    public void removeDisplay() {
        if (!display.isDead()) display.remove();
    }
}