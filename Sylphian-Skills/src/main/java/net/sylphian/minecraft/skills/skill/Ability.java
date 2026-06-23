package net.sylphian.minecraft.skills.skill;

/**
 * Metadata contract for a single ability within a skill.
 *
 * <p>Abilities are the individual perks unlocked as a player levels up a skill.
 * This interface carries only the data the framework needs to display them
 * (e.g. in a skill browser GUI); all gameplay logic lives in the concrete
 * implementation inside the owning plugin.</p>
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
