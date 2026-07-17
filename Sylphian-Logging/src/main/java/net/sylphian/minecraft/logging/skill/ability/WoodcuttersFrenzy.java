package net.sylphian.minecraft.logging.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.logging.config.LoggingSkillConfig;
import net.sylphian.minecraft.logging.skill.trigger.LogHarvestTrigger;
import net.sylphian.minecraft.skills.service.ActiveBuffTracker;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active unlocked at level 18.
 *
 * <p>For a timed buff, the player gains Haste and doubled logging XP.</p>
 */
public final class WoodcuttersFrenzy implements ActiveAbility, PassiveAbility {

    public static final String COOLDOWN_ID = "logging:woodcutters-frenzy";
    public static final String BUFF_ID     = "logging:woodcutters-frenzy-buff";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<LoggingSkillConfig> config;
    private final CooldownManager cooldowns;
    private final ActiveBuffTracker buffs;
    private final Plugin plugin;

    /**
     * @param config    supplier for the current config snapshot
     * @param cooldowns the shared cooldown manager
     * @param buffs     the active buff tracker
     * @param plugin    the owning plugin, used to schedule buff expiry
     */
    public WoodcuttersFrenzy(Supplier<LoggingSkillConfig> config, CooldownManager cooldowns,
                             ActiveBuffTracker buffs, Plugin plugin) {
        this.config = config;
        this.cooldowns = cooldowns;
        this.buffs = buffs;
        this.plugin = plugin;
    }

    @Override public String id()               { return COOLDOWN_ID; }
    @Override public String name()             { return "Woodcutter's Frenzy"; }
    @Override public String description()      { return "For a time, gain Haste and double logging XP."; }
    @Override public int    unlockLevel()      { return 18; }
    @Override public String triggerCondition() { return "While Woodcutter's Frenzy is active."; }

    @Override
    public void onActivate(Player player, UUID uuid) {
        if (buffs.hasBuff(uuid, BUFF_ID)) {
            player.sendActionBar(MINI.deserialize("<gold>Woodcutter's Frenzy <white>is already active!"));
            return;
        }
        long remaining = cooldowns.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Woodcutter's Frenzy: <white>" + (remaining / 1000) + "s remaining."));
            return;
        }
        LoggingSkillConfig cfg = config.get();
        int durationTicks = cfg.woodcuttersFrenzyDurationSeconds() * 20;

        buffs.addBuff(uuid, BUFF_ID);
        cooldowns.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.woodcuttersFrenzyCooldownSeconds()));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, durationTicks, cfg.woodcuttersFrenzyHasteAmplifier(), true, true));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            buffs.removeBuff(uuid, BUFF_ID);
            if (player.isOnline()) {
                player.sendActionBar(MINI.deserialize("<red>Woodcutter's Frenzy <white>has ended."));
            }
        }, durationTicks);

        player.sendActionBar(MINI.deserialize(
                "<gold>Woodcutter's Frenzy! <yellow>" + cfg.woodcuttersFrenzyDurationSeconds()
                + "s <white>of Haste and double XP."));
    }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof LogHarvestTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        if (!buffs.hasBuff(uuid, BUFF_ID)) return;
        NodeHarvestEvent event = ((LogHarvestTrigger) trigger).event();
        double multiplier = config.get().woodcuttersFrenzyXpMultiplier();
        event.setXpMultiplier(event.getXpMultiplier() * multiplier);
        trigger.record(name(), String.format("x%.1f XP (buff active)", multiplier), true);
    }

    @Override
    public String selectionStatus(UUID uuid) {
        if (buffs.hasBuff(uuid, BUFF_ID)) return "<gold>Active!";
        long s = cooldowns.getRemainingSeconds(uuid, COOLDOWN_ID);
        return s > 0 ? "<red>" + s + "s" : "<green>Ready";
    }

    @Override
    public StatusLevel statusLevel(UUID uuid) {
        if (buffs.hasBuff(uuid, BUFF_ID)) return StatusLevel.ACTIVE;
        return cooldowns.isOnCooldown(uuid, COOLDOWN_ID) ? StatusLevel.ON_COOLDOWN : StatusLevel.READY;
    }
}
