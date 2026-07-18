package net.sylphian.minecraft.mining.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.gathering.node.LootEntry;
import net.sylphian.minecraft.gathering.node.NodeModifier;
import net.sylphian.minecraft.gathering.node.NodeType;
import net.sylphian.minecraft.mining.config.MiningSkillConfig;
import net.sylphian.minecraft.mining.skill.trigger.OreHarvestTrigger;
import net.sylphian.minecraft.skills.service.ActiveBuffTracker;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActivationResult;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active unlocked at level 15.
 *
 * <p>For a timed buff, every mining harvest also drops the node's modifier bonus
 * loot, even when the node rolled no modifier.</p>
 */
public final class ProspectorsEye implements ActiveAbility, PassiveAbility {

    public static final String COOLDOWN_ID = "mining:prospectors-eye";
    public static final String BUFF_ID     = "mining:prospectors-eye-buff";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<MiningSkillConfig> config;
    private final CooldownManager cooldowns;
    private final ActiveBuffTracker buffs;
    private final Plugin plugin;

    /**
     * @param config    supplier for the current config snapshot
     * @param cooldowns the shared cooldown manager
     * @param buffs     the active buff tracker
     * @param plugin    the owning plugin, used to schedule buff expiry
     */
    public ProspectorsEye(Supplier<MiningSkillConfig> config, CooldownManager cooldowns,
                          ActiveBuffTracker buffs, Plugin plugin) {
        this.config = config;
        this.cooldowns = cooldowns;
        this.buffs = buffs;
        this.plugin = plugin;
    }

    @Override public String id()               { return COOLDOWN_ID; }
    @Override public String name()             { return "Prospector's Eye"; }
    @Override public String description()      { return "For " + config.get().prospectorsEyeDurationSeconds() + " seconds, every harvest also drops modifier bonus loot."; }
    @Override public int    unlockLevel()      { return 15; }
    @Override public String triggerCondition() { return "While Prospector's Eye is active."; }

    @Override
    public ActivationResult onActivate(Player player, UUID uuid) {
        if (buffs.hasBuff(uuid, BUFF_ID)) {
            player.sendActionBar(MINI.deserialize("<gold>Prospector's Eye <white>is already active!"));
            return ActivationResult.blocked();
        }
        long remaining = cooldowns.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Prospector's Eye: <white>" + (remaining / 1000) + "s remaining."));
            return ActivationResult.blocked();
        }
        MiningSkillConfig cfg = config.get();
        buffs.addBuff(uuid, BUFF_ID);
        cooldowns.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.prospectorsEyeCooldownSeconds()));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            buffs.removeBuff(uuid, BUFF_ID);
            if (player.isOnline()) {
                player.sendActionBar(MINI.deserialize("<red>Prospector's Eye <white>has ended."));
            }
        }, cfg.prospectorsEyeDurationSeconds() * 20L);

        player.sendActionBar(MINI.deserialize(
                "<gold>Prospector's Eye! <yellow>" + cfg.prospectorsEyeDurationSeconds()
                + "s <white>of modifier bonus loot."));
        return ActivationResult.used("buff active " + cfg.prospectorsEyeDurationSeconds() + "s");
    }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof OreHarvestTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        if (!buffs.hasBuff(uuid, BUFF_ID)) return;
        NodeHarvestEvent event = ((OreHarvestTrigger) trigger).event();

        List<LootEntry> bonus = modifierBonusLoot(event.node().type());
        if (bonus.isEmpty()) return;

        for (LootEntry entry : bonus) event.addBonusLoot(entry);
        trigger.record(name(), "+" + bonus.size() + " modifier bonus loot (buff active)", true);
    }

    private static List<LootEntry> modifierBonusLoot(NodeType type) {
        for (NodeModifier modifier : type.modifiers()) {
            if (!modifier.bonusLoot().isEmpty()) return modifier.bonusLoot();
        }
        return List.of();
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
