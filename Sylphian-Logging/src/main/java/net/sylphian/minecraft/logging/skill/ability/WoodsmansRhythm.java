package net.sylphian.minecraft.logging.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.logging.config.LoggingSkillConfig;
import net.sylphian.minecraft.logging.skill.LoggingStreak;
import net.sylphian.minecraft.logging.skill.trigger.LogHarvestTrigger;
import net.sylphian.minecraft.logging.skill.trigger.LogHarvestedTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive unlocked at level 30.
 *
 * <p>Consecutive logging harvests within a short window build a streak, each
 * stack adding a flat XP bonus up to a cap. Applied on the pre-harvest trigger
 * and advanced on the post-harvest trigger.</p>
 */
public final class WoodsmansRhythm implements PassiveAbility {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<LoggingSkillConfig> config;
    private final Map<UUID, LoggingStreak> streaks;

    /**
     * @param config  supplier for the current config snapshot
     * @param streaks shared per-player streak state, owned by the skill
     */
    public WoodsmansRhythm(Supplier<LoggingSkillConfig> config, Map<UUID, LoggingStreak> streaks) {
        this.config = config;
        this.streaks = streaks;
    }

    @Override public String id()               { return "logging:woodsmans-rhythm"; }
    @Override public String name()             { return "Woodsman's Rhythm"; }
    @Override public String description()      { return "Consecutive logging builds a streak, each stack adding +10% XP (max +50%)."; }
    @Override public int    unlockLevel()      { return 30; }
    @Override public String triggerCondition() { return "On logging nodes in quick succession."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof LogHarvestTrigger || trigger instanceof LogHarvestedTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        if (trigger instanceof LogHarvestTrigger harvest) {
            applyXp(uuid, harvest.event(), trigger);
        } else if (trigger instanceof LogHarvestedTrigger) {
            advance(player, uuid);
        }
    }

    private void applyXp(UUID uuid, NodeHarvestEvent event, PassiveTrigger trigger) {
        LoggingStreak streak = streaks.get(uuid);
        int stacks = streak != null ? streak.stacks() : 0;
        if (stacks <= 0) return;

        LoggingSkillConfig cfg = config.get();
        double multiplier = 1.0 + stacks * cfg.woodsmansRhythmXpPerStack();
        event.setXpMultiplier(event.getXpMultiplier() * multiplier);
        trigger.record(name(), String.format("x%.2f XP (%d stack%s)", multiplier, stacks, stacks == 1 ? "" : "s"));
    }

    private void advance(Player player, UUID uuid) {
        LoggingSkillConfig cfg = config.get();
        long now = System.currentTimeMillis();
        LoggingStreak current = streaks.get(uuid);

        boolean inWindow = current != null && now - current.lastMillis() <= cfg.woodsmansRhythmWindowSeconds() * 1000L;
        int stacks = inWindow ? Math.min(current.stacks() + 1, cfg.woodsmansRhythmMaxStacks()) : 1;
        streaks.put(uuid, new LoggingStreak(stacks, now));

        if (inWindow && stacks > current.stacks()) {
            player.sendActionBar(MINI.deserialize(
                    "<gold>Woodsman's Rhythm: <white>Stack " + stacks + "/" + cfg.woodsmansRhythmMaxStacks()
                    + " <gray>(+" + (int) (stacks * cfg.woodsmansRhythmXpPerStack() * 100) + "% XP)"));
        }
    }
}
