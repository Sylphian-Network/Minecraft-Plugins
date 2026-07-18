package net.sylphian.minecraft.mining.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.mining.config.MiningSkillConfig;
import net.sylphian.minecraft.mining.skill.MiningStreak;
import net.sylphian.minecraft.mining.skill.trigger.OreHarvestTrigger;
import net.sylphian.minecraft.mining.skill.trigger.OreHarvestedTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive unlocked at level 30.
 *
 * <p>Consecutive mining harvests within a short window build a streak, each
 * stack adding a flat yield bonus up to a cap. The streak is applied on the
 * pre-harvest trigger and advanced on the post-harvest trigger, so a harvest
 * benefits from the streak the previous one built. Times out on its own or
 * resets on quit.</p>
 */
public final class SteadyRhythm implements PassiveAbility {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<MiningSkillConfig> config;
    private final Map<UUID, MiningStreak> streaks;

    /**
     * @param config  supplier for the current config snapshot
     * @param streaks shared per-player streak state, owned by the skill
     */
    public SteadyRhythm(Supplier<MiningSkillConfig> config, Map<UUID, MiningStreak> streaks) {
        this.config = config;
        this.streaks = streaks;
    }

    @Override public String id()               { return "mining:steady-rhythm"; }
    @Override public String name()             { return "Steady Rhythm"; }
    @Override public String description()      { return "Consecutive mining builds a streak, each stack adding +10% yield (max +50%)."; }
    @Override public int    unlockLevel()      { return 30; }
    @Override public String triggerCondition() { return "On mining nodes in quick succession."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof OreHarvestTrigger || trigger instanceof OreHarvestedTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        if (trigger instanceof OreHarvestTrigger harvest) {
            applyYield(uuid, harvest.event(), trigger);
        } else if (trigger instanceof OreHarvestedTrigger) {
            advance(player, uuid);
        }
    }

    private void applyYield(UUID uuid, NodeHarvestEvent event, PassiveTrigger trigger) {
        MiningStreak streak = streaks.get(uuid);
        int stacks = streak != null ? streak.stacks() : 0;
        if (stacks <= 0) return;

        MiningSkillConfig cfg = config.get();
        double multiplier = 1.0 + stacks * cfg.steadyRhythmYieldPerStack();
        event.setYieldMultiplier(event.getYieldMultiplier() * multiplier);
        trigger.record(name(), String.format("x%.2f yield (%d stack%s)", multiplier, stacks, stacks == 1 ? "" : "s"));
    }

    private void advance(Player player, UUID uuid) {
        MiningSkillConfig cfg = config.get();
        long now = System.currentTimeMillis();
        MiningStreak current = streaks.get(uuid);

        boolean inWindow = current != null && now - current.lastMillis() <= cfg.steadyRhythmWindowSeconds() * 1000L;
        int stacks = inWindow ? Math.min(current.stacks() + 1, cfg.steadyRhythmMaxStacks()) : 1;
        streaks.put(uuid, new MiningStreak(stacks, now));

        if (inWindow && stacks > current.stacks()) {
            player.sendActionBar(MINI.deserialize(
                    "<gold>Steady Rhythm: <white>Stack " + stacks + "/" + cfg.steadyRhythmMaxStacks()
                    + " <gray>(+" + (int) (stacks * cfg.steadyRhythmYieldPerStack() * 100) + "% yield)"));
        }
    }
}
