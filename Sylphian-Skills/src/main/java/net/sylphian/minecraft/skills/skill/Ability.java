package net.sylphian.minecraft.skills.skill;

/**
 * Base contract for a single ability within a skill.
 *
 * <p>An ability is any perk unlocked as a player levels up a skill. Concrete
 * subtypes determine how it is invoked:</p>
 * <ul>
 *   <li>{@link ActiveAbility} — manually triggered by the player via the
 *       sneak-right-click selection GUI.</li>
 *   <li>{@link PassiveAbility} — automatically dispatched by
 *       {@link AbstractSkill#firePassives} when a matching game event fires.</li>
 * </ul>
 *
 * <p>This interface intentionally contains only the metadata that applies to
 * every ability regardless of type. Use {@code instanceof} checks to access
 * type-specific methods.</p>
 */
public interface Ability {

    /**
     * @return the namespaced identifier, e.g. {@code "fishing:patient-angler"}
     */
    String id();

    /**
     * @return the player-facing display name, e.g. {@code "Patient Angler"}
     */
    String name();

    /**
     * @return a short player-facing description of what the ability does
     */
    String description();

    /**
     * @return the skill level at which this ability is unlocked
     */
    int unlockLevel();
}
