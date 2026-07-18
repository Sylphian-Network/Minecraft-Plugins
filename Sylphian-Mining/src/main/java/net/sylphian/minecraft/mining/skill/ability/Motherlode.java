package net.sylphian.minecraft.mining.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.node.LootEntry;
import net.sylphian.minecraft.gathering.node.LootTable;
import net.sylphian.minecraft.mining.config.MiningSkillConfig;
import net.sylphian.minecraft.mining.skill.trigger.OreHarvestTrigger;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActivationResult;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active unlocked at level 25.
 *
 * <p>Pending: the player's next mining harvest rolls the node's loot table a
 * number of extra times, each roll added as bonus loot. Distinct from Vein
 * Surge, which multiplies a single roll.</p>
 */
public final class Motherlode implements ActiveAbility {

    public static final String COOLDOWN_ID = "mining:motherlode";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<MiningSkillConfig> config;
    private final CooldownManager cooldowns;
    private final Set<UUID> pending;
    private final Random random = new Random();

    /**
     * @param config    supplier for the current config snapshot
     * @param cooldowns the shared cooldown manager
     * @param pending   the set tracking whose next mine gets extra rolls
     */
    public Motherlode(Supplier<MiningSkillConfig> config, CooldownManager cooldowns, Set<UUID> pending) {
        this.config = config;
        this.cooldowns = cooldowns;
        this.pending = pending;
    }

    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Motherlode"; }
    @Override public String description() { return "Your next mining harvest rolls the loot table several extra times."; }
    @Override public int    unlockLevel() { return 25; }

    @Override
    public ActivationResult onActivate(Player player, UUID uuid) {
        if (pending.contains(uuid)) {
            player.sendActionBar(MINI.deserialize("<yellow>Motherlode <white>is already pending your next harvest."));
            return ActivationResult.blocked();
        }
        long remaining = cooldowns.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Motherlode: <white>" + (remaining / 1000) + "s remaining."));
            return ActivationResult.blocked();
        }
        pending.add(uuid);
        cooldowns.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(config.get().motherlodeCooldownSeconds()));
        player.sendActionBar(MINI.deserialize("<aqua>Motherlode <white>ready! Your next harvest strikes rich."));
        return ActivationResult.used("primed: next harvest rolls extra loot");
    }

    /**
     * Rolls the node's loot table the configured number of extra times and adds
     * each result as bonus loot, if this harvest is pending.
     *
     * @param uuid    the harvesting player's UUID
     * @param trigger the pre-harvest trigger to mutate and record on
     */
    public void applyOnHarvest(UUID uuid, OreHarvestTrigger trigger) {
        if (!pending.remove(uuid)) return;

        LootTable loot = trigger.event().node().type().loot();
        int rolls = config.get().motherlodeExtraRolls();
        int added = 0;
        for (int i = 0; i < rolls; i++) {
            LootEntry rolled = loot.roll(random);
            if (rolled != null) {
                trigger.event().addBonusLoot(rolled);
                added++;
            }
        }
        if (added > 0) trigger.record(name(), added + " extra loot roll" + (added == 1 ? "" : "s"), true);
    }

    @Override
    public String selectionStatus(UUID uuid) {
        if (pending.contains(uuid)) return "<yellow>Pending";
        long s = cooldowns.getRemainingSeconds(uuid, COOLDOWN_ID);
        return s > 0 ? "<red>" + s + "s" : "<green>Ready";
    }

    @Override
    public StatusLevel statusLevel(UUID uuid) {
        if (pending.contains(uuid)) return StatusLevel.PENDING;
        return cooldowns.isOnCooldown(uuid, COOLDOWN_ID) ? StatusLevel.ON_COOLDOWN : StatusLevel.READY;
    }
}
