package net.sylphian.minecraft.skills.skill;

/**
 * Represents the display state of an {@link ActiveAbility} in the selection GUI.
 *
 * <p>Returned by {@link ActiveAbility#statusLevel} so the
 * {@code ActiveAbilityCoordinator} can pick an appropriate display material
 * without parsing the {@link ActiveAbility#selectionStatus} MiniMessage string.</p>
 */
public enum StatusLevel {

    /** The ability is available for use. */
    READY,

    /** The ability has been activated and is waiting for the next qualifying event. */
    PENDING,

    /** The ability was recently used and cannot be activated yet. */
    ON_COOLDOWN,

    /** A timed buff from this ability is currently running. */
    ACTIVE
}
