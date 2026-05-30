package net.sylphian.minecraft.fishing.config;

/**
 * Immutable configuration for catch effects triggered when a fish of a
 * specific rarity is caught. Each effect type can be individually enabled
 * or disabled via config.yml.
 *
 * @param sound     sound effect configuration, or null if disabled
 * @param particle  particle effect configuration, or null if disabled
 * @param title     title configuration, or null if disabled
 * @param broadcast broadcast message configuration, or null if disabled
 */
public record RarityCatchEffects(SoundConfig sound, ParticleConfig particle, TitleConfig title, BroadcastConfig broadcast) {

    /**
     * Sound effect played to the catching player.
     *
     * @param name   the Bukkit sound name
     * @param volume the volume (0.0 to 1.0+)
     * @param pitch  the pitch (0.5 to 2.0)
     */
    public record SoundConfig(String name, float volume, float pitch) {}

    /**
     * Particle effect spawned at the hook location.
     *
     * @param type     the Bukkit particle type name
     * @param count    number of particles to spawn
     * @param offsetX  spread on the X axis
     * @param offsetY  spread on the Y axis
     * @param offsetZ  spread on the Z axis
     */
    public record ParticleConfig(String type, int count,
                                 double offsetX, double offsetY, double offsetZ) {}

    /**
     * Title shown to the catching player.
     *
     * @param title    the main title MiniMessage string
     * @param subtitle the subtitle MiniMessage string
     * @param fadeIn   fade-in duration in ticks
     * @param stay     display duration in ticks
     * @param fadeOut  fade-out duration in ticks
     */
    public record TitleConfig(String title, String subtitle,
                              int fadeIn, int stay, int fadeOut) {}

    /**
     * Server-wide broadcast message.
     * Supports {player} and {fish} placeholders.
     *
     * @param message the MiniMessage broadcast string
     */
    public record BroadcastConfig(String message) {}

    /** Returns an empty no-op effects config with all effects disabled. */
    public static RarityCatchEffects empty() {
        return new RarityCatchEffects(null, null, null, null);
    }
}