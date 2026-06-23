package net.sylphian.minecraft.fishing.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.skills.service.ActiveBuffTracker;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.Ability;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.UUID;

/**
 * Active perk unlocked at level 25.
 *
 * <p>For a configurable duration, all hook wait times are reduced by 60%
 * and the player earns double XP per catch. A buff marker is held in
 * {@link ActiveBuffTracker} for the duration and cleared on expiry.</p>
 */
public final class FishersFrenzy implements Ability {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Key used with {@link CooldownManager} to track the cooldown. */
    public static final String COOLDOWN_ID = "fishing:fishers-frenzy";
    /** Key used with {@link ActiveBuffTracker} to track the active buff. */
    public static final String BUFF_ID     = "fishing:frenzy-buff";

    @Override public String id()           { return COOLDOWN_ID; }
    @Override public String name()         { return "Fisher's Frenzy"; }
    @Override public String description()  {
        return "For 60 seconds, bites come 60% faster and you earn double XP.";
    }
    @Override public int    unlockLevel()  { return 25; }

    /**
     * Activates Fisher's Frenzy: applies the buff, starts the cooldown, and
     * schedules the buff expiry task.
     *
     * @param player  the activating player
     * @param uuid    the player's UUID
     * @param cfg     current config snapshot
     * @param cd      cooldown manager
     * @param buffs   active buff tracker
     * @param plugin  the owning plugin (used for scheduling the expiry task)
     */
    public void activate(Player player, UUID uuid, FishingSkillConfig cfg,
                         CooldownManager cd, ActiveBuffTracker buffs, Plugin plugin) {
        buffs.addBuff(uuid, BUFF_ID);
        cd.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.fishersFrenzyCooldownSeconds()));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            buffs.removeBuff(uuid, BUFF_ID);
            if (player.isOnline()) {
                player.sendActionBar(MINI.deserialize("<red>Fisher's Frenzy <white>has ended."));
            }
        }, cfg.fishersFrenzyDurationSeconds() * 20L);

        player.sendActionBar(MINI.deserialize(
                "<gold>Fisher's Frenzy! <yellow>" + cfg.fishersFrenzyDurationSeconds()
                + "s <white>of faster bites and double XP."));
    }

    /**
     * @param uuid  the player's UUID
     * @param buffs active buff tracker
     * @return {@code true} if Fisher's Frenzy is currently active for this player
     */
    public boolean isActive(UUID uuid, ActiveBuffTracker buffs) {
        return buffs.hasBuff(uuid, BUFF_ID);
    }

    /**
     * @param uuid  the player's UUID
     * @param buffs active buff tracker
     * @param cfg   current config snapshot
     * @return the fractional wait-time reduction while active, or 0.0 if inactive
     */
    public double reductionFraction(UUID uuid, ActiveBuffTracker buffs, FishingSkillConfig cfg) {
        return isActive(uuid, buffs) ? cfg.fishersFrenzyReductionPercent() / 100.0 : 0.0;
    }

    /**
     * @param uuid  the player's UUID
     * @param buffs active buff tracker
     * @return 2.0 while the buff is active, 1.0 otherwise
     */
    public double xpMultiplier(UUID uuid, ActiveBuffTracker buffs) {
        return isActive(uuid, buffs) ? 2.0 : 1.0;
    }
}
