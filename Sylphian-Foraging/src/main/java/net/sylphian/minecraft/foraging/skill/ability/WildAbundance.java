package net.sylphian.minecraft.foraging.skill.ability;

import net.sylphian.minecraft.foraging.config.ForagingSkillConfig;
import net.sylphian.minecraft.foraging.skill.trigger.ForageHarvestTrigger;
import net.sylphian.minecraft.foraging.skill.trigger.ForageHarvestedTrigger;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive unlocked at level 20.
 *
 * <p>Yield scales with the variety of foraging nodes harvested recently: each
 * distinct node id gathered within the window adds a flat bonus, up to a cap.
 * Rewards roaming across node types over farming one bush.</p>
 */
public final class WildAbundance implements PassiveAbility {

    private final Supplier<ForagingSkillConfig> config;
    private final Map<UUID, Map<String, Long>> recent;

    /**
     * @param config supplier for the current config snapshot
     * @param recent shared per-player map of node id to last-harvest millis, owned by the skill
     */
    public WildAbundance(Supplier<ForagingSkillConfig> config, Map<UUID, Map<String, Long>> recent) {
        this.config = config;
        this.recent = recent;
    }

    @Override public String id()               { return "foraging:wild-abundance"; }
    @Override public String name()             { return "Wild Abundance"; }
    @Override public String description()      { return "Yield scales with the variety of nodes you foraged recently (up to +40%)."; }
    @Override public int    unlockLevel()      { return 20; }
    @Override public String triggerCondition() { return "On foraging varied nodes within a short time."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof ForageHarvestTrigger || trigger instanceof ForageHarvestedTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        if (trigger instanceof ForageHarvestTrigger harvest) {
            applyYield(uuid, harvest.event(), trigger);
        } else if (trigger instanceof ForageHarvestedTrigger harvested) {
            record(uuid, harvested.event().node().type().id());
        }
    }

    private void applyYield(UUID uuid, NodeHarvestEvent event, PassiveTrigger trigger) {
        ForagingSkillConfig cfg = config.get();
        int variety = distinctInWindow(uuid, cfg.wildAbundanceWindowSeconds() * 1000L);
        if (variety <= 0) return;

        int counted = Math.min(variety, cfg.wildAbundanceMaxVariety());
        double multiplier = 1.0 + counted * cfg.wildAbundanceYieldPerVariety();
        event.setYieldMultiplier(event.getYieldMultiplier() * multiplier);
        trigger.record(name(), String.format("x%.2f yield (%d node type%s)", multiplier, counted, counted == 1 ? "" : "s"));
    }

    private int distinctInWindow(UUID uuid, long windowMillis) {
        Map<String, Long> seen = recent.get(uuid);
        if (seen == null) return 0;
        long cutoff = System.currentTimeMillis() - windowMillis;
        seen.values().removeIf(last -> last < cutoff);
        return seen.size();
    }

    private void record(UUID uuid, String nodeId) {
        recent.computeIfAbsent(uuid, k -> new java.util.concurrent.ConcurrentHashMap<>())
                .put(nodeId, System.currentTimeMillis());
    }
}
