package net.sylphian.minecraft.foraging.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.foraging.config.ForagingSkillConfig;
import net.sylphian.minecraft.foraging.skill.trigger.ForageHarvestTrigger;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Passive unlocked at level 5.
 *
 * <p>Rolls a chance, scaling with level, to pick the node without depleting it,
 * leaving it available to harvest again.</p>
 */
public final class GentleTouch implements PassiveAbility {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<ForagingSkillConfig> config;
    private final ToIntFunction<UUID> levelOf;
    private final Random random = new Random();

    /**
     * @param config  supplier for the current config snapshot
     * @param levelOf lookup for a player's current foraging level
     */
    public GentleTouch(Supplier<ForagingSkillConfig> config, ToIntFunction<UUID> levelOf) {
        this.config = config;
        this.levelOf = levelOf;
    }

    @Override public String id()               { return "foraging:gentle-touch"; }
    @Override public String name()             { return "Gentle Touch"; }
    @Override public String description()      { return "A chance, growing with level, to pick without killing the plant."; }
    @Override public int    unlockLevel()      { return 5; }
    @Override public String triggerCondition() { return "On harvesting a foraging node."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof ForageHarvestTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        NodeHarvestEvent event = ((ForageHarvestTrigger) trigger).event();
        ForagingSkillConfig cfg = config.get();

        double chance = cfg.gentleTouchBaseChance() + cfg.gentleTouchChancePerLevel() * levelOf.applyAsInt(uuid);
        if (random.nextDouble() >= chance) return;

        event.setDeplete(false);
        trigger.record(name(), "node not depleted");
        player.sendActionBar(MINI.deserialize("<green>Gentle Touch: <white>the plant survives your harvest."));
    }
}
