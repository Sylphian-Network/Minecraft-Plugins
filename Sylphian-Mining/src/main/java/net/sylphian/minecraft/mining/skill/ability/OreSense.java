package net.sylphian.minecraft.mining.skill.ability;

import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.gathering.node.LootEntry;
import net.sylphian.minecraft.gathering.node.LootTable;
import net.sylphian.minecraft.mining.config.MiningSkillConfig;
import net.sylphian.minecraft.mining.skill.trigger.OreHarvestTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Passive unlocked at level 10.
 *
 * <p>Rolls a chance, scaling with level, to drop one bonus unit of the node's
 * own base loot.</p>
 */
public final class OreSense implements PassiveAbility {

    private final Supplier<MiningSkillConfig> config;
    private final ToIntFunction<UUID> levelOf;
    private final Random random = new Random();

    /**
     * @param config  supplier for the current config snapshot
     * @param levelOf lookup for a player's current mining level
     */
    public OreSense(Supplier<MiningSkillConfig> config, ToIntFunction<UUID> levelOf) {
        this.config = config;
        this.levelOf = levelOf;
    }

    @Override public String id()               { return "mining:ore-sense"; }
    @Override public String name()             { return "Ore Sense"; }
    @Override public String description()      { return "A chance, growing with level, to drop a bonus unit of the ore."; }
    @Override public int    unlockLevel()      { return 10; }
    @Override public String triggerCondition() { return "On harvesting a mining node."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof OreHarvestTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        NodeHarvestEvent event = ((OreHarvestTrigger) trigger).event();
        MiningSkillConfig cfg = config.get();

        LootTable loot = event.node().type().loot();
        if (loot.entries().isEmpty()) return;

        double chance = cfg.oreSenseBaseChance() + cfg.oreSenseChancePerLevel() * levelOf.applyAsInt(uuid);
        if (random.nextDouble() >= chance) return;

        LootEntry base = loot.entries().getFirst();
        event.addBonusLoot(new LootEntry(base.itemId(), 1, 1, 1));
        trigger.record(name(), "+1 " + base.itemId() + " (" + (int) (chance * 100) + "% roll hit)");
    }
}
