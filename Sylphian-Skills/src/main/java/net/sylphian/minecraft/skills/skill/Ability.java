package net.sylphian.minecraft.skills.skill;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Metadata and activation contract for a single ability within a skill.
 *
 * <p>Abilities are the individual perks unlocked as a player levels up a skill.
 * The framework uses this interface to display abilities in the GUI and, for
 * active abilities, to delegate activation when the player triggers the sneak-scroll
 * and sneak-right-click gestures.</p>
 *
 * <p>Passive abilities only need to implement the metadata methods; the default
 * implementations of the activation hooks are no-ops.</p>
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
     * @return {@code true} if this ability is manually triggered by the player;
     *         {@code false} if it is always active passively
     */
    default boolean isActive() {
        return false;
    }

    /**
     * @return instructions telling the player how to activate this ability.
     *         Passive abilities return {@code "Always active."}
     */
    default String activation() {
        return "Always active.";
    }

    /**
     * @return the skill level at which this ability is unlocked
     */
    int unlockLevel();

    /**
     * Called by the framework when the player triggers this ability via
     * sneak-right-click. The implementation is responsible for checking whether
     * the ability can fire right now and sending appropriate feedback.
     * Default no-op for passive abilities.
     *
     * @param player the player who triggered the ability
     * @param uuid   the player's UUID
     */
    default void onActivate(Player player, UUID uuid) {}

    /**
     * Returns a short MiniMessage string describing the ability's current state,
     * shown in the action bar during sneak-scroll selection.
     * Examples: {@code "<green>Ready"}, {@code "<red>12s"}, {@code "<yellow>Pending"}.
     * Default implementation always returns ready; override for active abilities.
     *
     * @param uuid the player's UUID
     * @return a MiniMessage status string
     */
    default String selectionStatus(UUID uuid) {
        return "<green>Ready";
    }
}
