package net.sylphian.minecraft.skills.skill;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Extension of {@link Ability} for abilities that are manually triggered by the player.
 *
 * <p>Active abilities participate in the {@code ActiveAbilityCoordinator} framework:
 * when the player sneaks and right-clicks while holding the skill's trigger material,
 * the coordinator opens a selection GUI populated from all unlocked
 * {@code ActiveAbility} instances for that skill. Clicking an ability calls
 * {@link #onActivate}.</p>
 *
 * <p>The default {@link #activation()} text matches the coordinator's gesture so
 * individual abilities do not need to repeat it. Override only if an ability has
 * a genuinely different or additional activation mechanic.</p>
 *
 * <p>The default {@link #selectionStatus} returns {@code "<green>Ready"}. Override
 * to reflect cooldown, pending, or buff state.</p>
 */
public interface ActiveAbility extends Ability {

    /**
     * Player-facing instructions for how to trigger this ability.
     * Defaults to the standard coordinator gesture text.
     *
     * @return a short activation description shown in the skill detail GUI
     */
    default String activation() {
        return "Sneak + right-click to open ability menu.";
    }

    /**
     * Called by the framework when the player selects this ability from the GUI.
     * Implementations are responsible for checking cooldown / pending state and
     * sending appropriate feedback to the player.
     *
     * @param player the player who triggered the ability
     * @param uuid   the player's UUID
     */
    void onActivate(Player player, UUID uuid);

    /**
     * Short MiniMessage string reflecting this ability's current state, shown as
     * the item lore in the selection GUI.
     * Examples: {@code "<green>Ready"}, {@code "<red>12s"}, {@code "<yellow>Pending"}.
     *
     * @param uuid the player's UUID
     * @return a MiniMessage status string
     */
    default String selectionStatus(UUID uuid) {
        return "<green>Ready";
    }
}
