package net.sylphian.minecraft.fishing.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.Ability;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Active perk unlocked at level 5.
 *
 * <p>When activated, the player's next cast will use shortened hook wait times,
 * causing a bite to occur within 3-5 seconds instead of the normal window.</p>
 */
public final class PatientAngler implements Ability {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Cooldown key used with {@link CooldownManager}. */
    public static final String COOLDOWN_ID = "fishing:patient-angler";

    @Override public String id()           { return COOLDOWN_ID; }
    @Override public String name()         { return "Patient Angler"; }
    @Override public String description()  { return "Your next cast will bite in 3-5 seconds."; }
    @Override public int    unlockLevel()  { return 5; }

    /**
     * Marks the next cast as a Patient Angler cast and starts the cooldown.
     *
     * @param player  the activating player
     * @param uuid    the player's UUID
     * @param cfg     current config snapshot
     * @param cd      cooldown manager
     * @param pending the set tracking whose next cast is affected
     */
    public void activate(Player player, UUID uuid, FishingSkillConfig cfg,
                         CooldownManager cd, Set<UUID> pending) {
        pending.add(uuid);
        cd.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.patientAnglerCooldownSeconds()));
        player.sendActionBar(MINI.deserialize(
                "<aqua>Patient Angler <white>ready! Your next cast will bite quickly."));
    }

    /**
     * Applies the shortened wait times to the hook if this player's cast is pending.
     * Removes the pending marker regardless of outcome so it does not carry over.
     *
     * @param hook    the newly cast hook
     * @param uuid    the casting player's UUID
     * @param cfg     current config snapshot
     * @param pending the set tracking whose next cast is affected
     */
    public void applyOnCast(FishHook hook, UUID uuid, FishingSkillConfig cfg, Set<UUID> pending) {
        if (!pending.remove(uuid)) return;
        hook.setMinWaitTime(cfg.patientAnglerMinTicks());
        hook.setMaxWaitTime(cfg.patientAnglerMaxTicks());
    }
}
