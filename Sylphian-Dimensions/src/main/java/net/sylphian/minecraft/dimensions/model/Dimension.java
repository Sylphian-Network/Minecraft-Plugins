package net.sylphian.minecraft.dimensions.model;

/**
 * An immutable dimension definition parsed from config.
 * Every dimension is template-based; the live Bukkit world is resolved
 * through the DimensionManager, not held here.
 *
 * @param name            the dimension name used in commands and config
 * @param template        the template folder name under {@code templates/}
 * @param templateVersion the template version; a mismatch with the active copy forces a re-copy
 * @param spawnPoint      the entry point inside the dimension world
 * @param chunkBoundsX    the built area width in chunks, centered on 0,0
 * @param chunkBoundsZ    the built area depth in chunks, centered on 0,0
 * @param ruleset         the rules enforced inside this dimension
 */
public record Dimension(
        String name,
        String template,
        int templateVersion,
        SpawnPoint spawnPoint,
        int chunkBoundsX,
        int chunkBoundsZ,
        DimensionRuleset ruleset) {

    /**
     * Returns a MiniMessage summary of template, spawn, and bounds, for admin tooling.
     *
     * @return the MiniMessage summary, two lines
     */
    public String describe() {
        return "<gray>Template: <white>" + template + " <gray>version <white>" + templateVersion + "\n"
                + "<gray>Spawn: <white>" + spawnPoint.x() + ", " + spawnPoint.y() + ", " + spawnPoint.z()
                + " <gray>Bounds: <white>" + chunkBoundsX + "x" + chunkBoundsZ + " chunks";
    }

    /**
     * A world-independent spawn position.
     *
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     * @param yaw   the horizontal facing
     * @param pitch the vertical facing
     */
    public record SpawnPoint(double x, double y, double z, float yaw, float pitch) {
    }
}
