package net.sylphian.minecraft.foraging.skill.ability;

import net.sylphian.minecraft.foraging.config.ForagingSkillConfig;
import net.sylphian.minecraft.foraging.skill.trigger.ForageHarvestTrigger;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.gathering.node.LootEntry;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive unlocked at level 12.
 *
 * <p>Rolls a flat chance to also drop a wild herb on a foraging harvest.</p>
 */
public final class Herbalist implements PassiveAbility {

    private final Supplier<ForagingSkillConfig> config;
    private final Random random = new Random();

    /**
     * @param config supplier for the current config snapshot
     */
    public Herbalist(Supplier<ForagingSkillConfig> config) {
        this.config = config;
    }

    @Override public String id()               { return "foraging:herbalist"; }
    @Override public String name()             { return "Herbalist"; }
    @Override public String description()      { return "A chance to also gather a wild herb."; }
    @Override public int    unlockLevel()      { return 12; }
    @Override public String triggerCondition() { return "On harvesting a foraging node."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof ForageHarvestTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        NodeHarvestEvent event = ((ForageHarvestTrigger) trigger).event();
        ForagingSkillConfig cfg = config.get();

        if (random.nextDouble() >= cfg.herbalistChance()) return;
        event.addBonusLoot(new LootEntry(cfg.herbalistItem(), 1, 1, 1));
        trigger.record(name(), "+1 " + cfg.herbalistItem());
    }
}
