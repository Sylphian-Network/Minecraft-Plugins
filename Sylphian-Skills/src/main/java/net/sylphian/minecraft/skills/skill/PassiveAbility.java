package net.sylphian.minecraft.skills.skill;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Extension of {@link Ability} for passives that participate in the passive
 * framework routing.
 *
 * <p>Implement this interface instead of plain {@link Ability} when a passive
 * should be invoked automatically by
 * {@link AbstractSkill#firePassives(PassiveTrigger, Player, UUID)} rather than
 * being called ad-hoc from the skill's event handler.</p>
 *
 * <p>A passive may react to more than one trigger type; {@link #accepts} is
 * checked before each dispatch so a single class can handle multiple trigger
 * types with an {@code instanceof} switch inside {@link #onPassiveTrigger}.</p>
 */
public interface PassiveAbility extends Ability {

    /**
     * Returns {@code true} if this passive should fire for the given trigger.
     *
     * <p>Typically implemented as one or more {@code instanceof} checks:
     * <pre>
     *     return trigger instanceof FishCastTrigger
     *         || trigger instanceof FishCatchTrigger;
     * </pre>
     * </p>
     *
     * @param trigger the trigger token about to be dispatched
     * @return {@code true} if this passive handles the trigger
     */
    boolean accepts(PassiveTrigger trigger);

    /**
     * Called by {@link AbstractSkill#firePassives} when this passive's trigger
     * fires and the player's level meets {@link #unlockLevel()}.
     *
     * <p>The trigger token may be cast to the concrete type declared by
     * {@link #accepts}. Passives that aggregate outputs (e.g. timer reductions)
     * write results back into the trigger; the skill reads those results after
     * {@code firePassives} returns.</p>
     *
     * @param player  the player the trigger concerns
     * @param uuid    the player's UUID
     * @param trigger the trigger token; cast to the expected concrete type
     */
    void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger);

    /**
     * Short player-facing description of when this passive fires, shown in the
     * skill detail GUI. Example: {@code "On casting a fishing rod."}
     *
     * @return a player-facing trigger description
     */
    String triggerCondition();
}
