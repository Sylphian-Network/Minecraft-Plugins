package net.sylphian.minecraft.fishing.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.fishing.skill.trigger.FishCastTrigger;
import net.sylphian.minecraft.fishing.skill.trigger.FishCatchTrigger;
import net.sylphian.minecraft.skills.service.ActiveBuffTracker;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active ability unlocked at level 25.
 *
 * <p>For a configurable duration, all hook wait times are reduced by 60%
 * and the player earns double XP per catch. A buff marker is held in
 * {@link ActiveBuffTracker} for the duration and cleared on expiry.</p>
 */
public final class FishersFrenzy implements ActiveAbility, PassiveAbility {

    /** Key used with {@link CooldownManager} to track the cooldown. */
    public static final String COOLDOWN_ID = "fishing:fishers-frenzy";
    /** Key used with {@link ActiveBuffTracker} to track the active buff. */
    public static final String BUFF_ID     = "fishing:frenzy-buff";
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final Supplier<FishingSkillConfig> config;
    private final CooldownManager cooldownManager;
    private final ActiveBuffTracker buffs;
    private final Plugin plugin;

    /**
     * @param config          supplier for the current config snapshot
     * @param cooldownManager the shared cooldown manager
     * @param buffs           the active buff tracker
     * @param plugin          the owning plugin, used to schedule the buff expiry task
     */
    public FishersFrenzy(Supplier<FishingSkillConfig> config,
                         CooldownManager cooldownManager,
                         ActiveBuffTracker buffs,
                         Plugin plugin) {
        this.config          = config;
        this.cooldownManager = cooldownManager;
        this.buffs           = buffs;
        this.plugin          = plugin;
    }
    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Fisher's Frenzy"; }
    @Override public String description() { return "For 60 seconds, bites come 60% faster and you earn double XP."; }
    @Override public int    unlockLevel() { return 25; }

    /**
     * Called by the framework when the player activates this ability.
     * Applies the buff, starts the cooldown, and schedules buff expiry.
     */
    @Override
    public void onActivate(Player player, UUID uuid) {
        if (isFrenzyActive(uuid)) {
            player.sendActionBar(MINI.deserialize("<gold>Fisher's Frenzy <white>is already active!"));
            return;
        }
        long remaining = cooldownManager.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize(
                    "<red>Fisher's Frenzy: <white>" + (remaining / 1000) + "s remaining."));
            return;
        }
        FishingSkillConfig cfg = config.get();
        buffs.addBuff(uuid, BUFF_ID);
        cooldownManager.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.fishersFrenzyCooldownSeconds()));

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
     * Short MiniMessage status string shown as item lore in the ability selection GUI.
     */
    @Override
    public String selectionStatus(UUID uuid) {
        if (isFrenzyActive(uuid)) return "<gold>Active!";
        long s = cooldownManager.getRemainingSeconds(uuid, COOLDOWN_ID);
        return s > 0 ? "<red>" + s + "s" : "<green>Ready";
    }

    @Override
    public StatusLevel statusLevel(UUID uuid) {
        if (isFrenzyActive(uuid)) return StatusLevel.ACTIVE;
        return cooldownManager.isOnCooldown(uuid, COOLDOWN_ID) ? StatusLevel.ON_COOLDOWN : StatusLevel.READY;
    }

    /**
     * @param uuid the player's UUID
     * @return {@code true} if Fisher's Frenzy is currently active for this player
     */
    public boolean isFrenzyActive(UUID uuid) {
        return buffs.hasBuff(uuid, BUFF_ID);
    }

    /**
     * @param uuid the player's UUID
     * @return 2.0 while the buff is active, 1.0 otherwise
     */
    private double xpMultiplier(UUID uuid) {
        return isFrenzyActive(uuid) ? 2.0 : 1.0;
    }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof FishCastTrigger || trigger instanceof FishCatchTrigger;
    }

    /**
     * Contributes cast timer reduction or XP multiplier while the buff is active.
     * Does nothing if Fisher's Frenzy is not currently running for this player.
     */
    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        if (!isFrenzyActive(uuid)) return;
        FishingSkillConfig cfg = config.get();
        if (trigger instanceof FishCastTrigger castTrigger) {
            double reduction = cfg.fishersFrenzyReductionPercent() / 100.0;
            castTrigger.addReduction(reduction);
            trigger.record(name(), "-" + (int)(reduction * 100) + "% cast timer (buff active)");
        } else if (trigger instanceof FishCatchTrigger catchTrigger) {
            catchTrigger.multiplyXp(2.0);
            trigger.record(name(), "x2.0 XP (buff active)");
        }
    }

    /** @return when this passive fires, shown in the skill detail GUI */
    @Override
    public String triggerCondition() {
        return "While Fisher's Frenzy buff is active.";
    }
}