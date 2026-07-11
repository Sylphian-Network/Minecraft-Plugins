package net.sylphian.minecraft.entities.config;

import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Spawn-time behavioural properties for a configured entity. Every property is
 * applied once when the entity spawns; none require a runtime listener.
 *
 * @param glowing       whether the entity has the glowing outline
 * @param baby          whether the entity spawns as a baby where supported
 * @param nameVisible   whether the custom name floats above the entity
 * @param persistent    whether the entity never despawns; false lets it despawn
 *                      naturally like a vanilla mob
 * @param potionEffects effects applied at spawn, with hidden particles and icon
 */
public record EntityProperties(
        boolean glowing,
        boolean baby,
        boolean nameVisible,
        boolean persistent,
        List<PotionSpec> potionEffects) {

    /** Fallback values for a definition with no {@code properties} block. */
    public static final EntityProperties DEFAULTS = new EntityProperties(false, false, true, false, List.of());

    /**
     * A single potion effect to apply at spawn.
     *
     * @param type          the resolved effect type
     * @param amplifier     the amplifier, 0 being level I
     * @param durationTicks the duration in ticks, or -1 for infinite
     */
    public record PotionSpec(PotionEffectType type, int amplifier, int durationTicks) {}
}
