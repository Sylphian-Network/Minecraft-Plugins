package net.sylphian.minecraft.fishing.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active ability unlocked at level 5.
 *
 * <p>When activated, the player's next cast will use shortened hook wait times,
 * causing a bite to occur within 3-5 seconds instead of the normal window.</p>
 */
public final class PatientAngler implements ActiveAbility {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Cooldown key used with {@link CooldownManager}. */
    public static final String COOLDOWN_ID = "fishing:patient-angler";

    private final Supplier<FishingSkillConfig> config;
    private final CooldownManager cooldownManager;
    private final Set<UUID> pendingSet;

    /**
     * @param config         supplier for the current config snapshot
     * @param cooldownManager the shared cooldown manager
     * @param pendingSet     the set tracking whose next cast is affected
     */
    public PatientAngler(Supplier<FishingSkillConfig> config,
                         CooldownManager cooldownManager,
                         Set<UUID> pendingSet) {
        this.config          = config;
        this.cooldownManager = cooldownManager;
        this.pendingSet      = pendingSet;
    }
    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Patient Angler"; }
    @Override public String description() { return "Your next cast will bite in 3-5 seconds."; }
    @Override public int    unlockLevel() { return 5; }

    /**
     * Called by the framework when the player activates this ability.
     * Checks cooldown and pending state before queuing the next fast cast.
     */
    @Override
    public void onActivate(Player player, UUID uuid) {
        if (pendingSet.contains(uuid)) {
            player.sendActionBar(MINI.deserialize(
                    "<yellow>Patient Angler <white>is already pending your next cast."));
            return;
        }
        if (cooldownManager.isOnCooldown(uuid, COOLDOWN_ID)) {
            player.sendActionBar(MINI.deserialize(
                    "<red>Patient Angler: <white>"
                    + cooldownManager.getRemainingSeconds(uuid, COOLDOWN_ID) + "s remaining."));
            return;
        }
        FishingSkillConfig cfg = config.get();
        pendingSet.add(uuid);
        cooldownManager.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.patientAnglerCooldownSeconds()));
        player.sendActionBar(MINI.deserialize(
                "<aqua>Patient Angler <white>ready! Your next cast will bite quickly."));
    }

    /**
     * Short MiniMessage status string shown as item lore in the ability selection GUI.
     */
    @Override
    public String selectionStatus(UUID uuid) {
        if (pendingSet.contains(uuid)) return "<yellow>Pending";
        long s = cooldownManager.getRemainingSeconds(uuid, COOLDOWN_ID);
        return s > 0 ? "<red>" + s + "s" : "<green>Ready";
    }

    @Override
    public StatusLevel statusLevel(UUID uuid) {
        if (pendingSet.contains(uuid)) return StatusLevel.PENDING;
        return cooldownManager.isOnCooldown(uuid, COOLDOWN_ID) ? StatusLevel.ON_COOLDOWN : StatusLevel.READY;
    }

    /**
     * Applies the shortened wait times to the hook if this player's cast is pending.
     * Removes the pending marker so it does not carry over to subsequent casts.
     *
     * @param hook    the newly cast hook
     * @param uuid    the casting player's UUID
     * @param trigger the cast trigger to record the contribution on
     */
    public void applyOnCast(FishHook hook, UUID uuid, PassiveTrigger trigger) {
        if (!pendingSet.remove(uuid)) return;
        FishingSkillConfig cfg = config.get();
        hook.setMinWaitTime(cfg.patientAnglerMinTicks());
        hook.setMaxWaitTime(cfg.patientAnglerMaxTicks());
        trigger.record(name(), "set hook to " + cfg.patientAnglerMinTicks() + "-" + cfg.patientAnglerMaxTicks() + "t", true);
    }
}
