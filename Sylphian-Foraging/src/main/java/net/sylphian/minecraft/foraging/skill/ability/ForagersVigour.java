package net.sylphian.minecraft.foraging.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.foraging.config.ForagingSkillConfig;
import net.sylphian.minecraft.foraging.skill.trigger.ForageHarvestTrigger;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.skills.service.ActiveBuffTracker;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActivationResult;
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
 * <p>For a timed buff, the player gains doubled foraging XP and Saturation.</p>
 */
public final class ForagersVigour implements ActiveAbility, PassiveAbility {

    public static final String COOLDOWN_ID = "foraging:foragers-vigour";
    public static final String BUFF_ID     = "foraging:foragers-vigour-buff";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<ForagingSkillConfig> config;
    private final CooldownManager cooldowns;
    private final ActiveBuffTracker buffs;
    private final Plugin plugin;

    /**
     * @param config    supplier for the current config snapshot
     * @param cooldowns the shared cooldown manager
     * @param buffs     the active buff tracker
     * @param plugin    the owning plugin, used to schedule buff expiry
     */
    public ForagersVigour(Supplier<ForagingSkillConfig> config, CooldownManager cooldowns,
                          ActiveBuffTracker buffs, Plugin plugin) {
        this.config = config;
        this.cooldowns = cooldowns;
        this.buffs = buffs;
        this.plugin = plugin;
    }

    @Override public String id()               { return COOLDOWN_ID; }
    @Override public String name()             { return "Forager's Vigour"; }
    @Override public String description()      { return "For a time, gain double foraging XP and Saturation."; }
    @Override public int    unlockLevel()      { return 18; }
    @Override public String triggerCondition() { return "While Forager's Vigour is active."; }

    @Override
    public ActivationResult onActivate(Player player, UUID uuid) {
        if (buffs.hasBuff(uuid, BUFF_ID)) {
            player.sendActionBar(MINI.deserialize("<gold>Forager's Vigour <white>is already active!"));
            return ActivationResult.blocked();
        }
        long remaining = cooldowns.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Forager's Vigour: <white>" + (remaining / 1000) + "s remaining."));
            return ActivationResult.blocked();
        }
        ForagingSkillConfig cfg = config.get();
        int durationTicks = cfg.foragersVigourDurationSeconds() * 20;

        buffs.addBuff(uuid, BUFF_ID);
        cooldowns.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.foragersVigourCooldownSeconds()));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, durationTicks, cfg.foragersVigourSaturationAmplifier(), true, true));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            buffs.removeBuff(uuid, BUFF_ID);
            if (player.isOnline()) {
                player.sendActionBar(MINI.deserialize("<red>Forager's Vigour <white>has ended."));
            }
        }, durationTicks);

        player.sendActionBar(MINI.deserialize(
                "<gold>Forager's Vigour! <yellow>" + cfg.foragersVigourDurationSeconds()
                + "s <white>of double XP and Saturation."));
        return ActivationResult.used("buff active " + cfg.foragersVigourDurationSeconds() + "s");
    }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof ForageHarvestTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        if (!buffs.hasBuff(uuid, BUFF_ID)) return;
        NodeHarvestEvent event = ((ForageHarvestTrigger) trigger).event();
        double multiplier = config.get().foragersVigourXpMultiplier();
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
