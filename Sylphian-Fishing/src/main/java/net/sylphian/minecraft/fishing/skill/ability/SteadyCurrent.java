package net.sylphian.minecraft.fishing.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.fishing.skill.trigger.FishCastTrigger;
import net.sylphian.minecraft.fishing.skill.trigger.FishCatchTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive perk unlocked at level 20.
 *
 * <p>Players who catch fish consecutively within a small radius build up
 * stacks of Steady Current, each reducing bite wait time further. Moving
 * too far between catches resets the streak to one stack.</p>
 *
 * <p>Reacts to two triggers:</p>
 * <ul>
 *   <li>{@link FishCatchTrigger} — updates the momentum streak and shows the
 *       action bar feedback.</li>
 *   <li>{@link FishCastTrigger} — contributes the current stack's timer
 *       reduction to the cast trigger's accumulator.</li>
 * </ul>
 */
public final class SteadyCurrent implements PassiveAbility {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<FishingSkillConfig> config;
    private final Map<UUID, CatchMomentum>     momentum;

    /**
     * @param config   supplier for the current config snapshot
     * @param momentum the shared per-player momentum state, owned by the skill
     */
    public SteadyCurrent(Supplier<FishingSkillConfig> config, Map<UUID, CatchMomentum> momentum) {
        this.config   = config;
        this.momentum = momentum;
    }

    @Override public String id()               { return "fishing:steady-current"; }
    @Override public String name()             { return "Steady Current"; }
    @Override public String description()      {
        return "Catching fish in the same spot builds stacks, each reducing bite time by 5% (max 5 stacks).";
    }
    @Override public int    unlockLevel()      { return 20; }
    @Override public String triggerCondition() { return "On catching a fish or casting a fishing rod."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof FishCatchTrigger || trigger instanceof FishCastTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        if (trigger instanceof FishCatchTrigger catchTrigger) {
            updateMomentum(player, uuid, catchTrigger);
        } else if (trigger instanceof FishCastTrigger castTrigger) {
            castTrigger.addReduction(reductionFraction(uuid));
        }
    }

    private void updateMomentum(Player player, UUID uuid, FishCatchTrigger trigger) {
        FishingSkillConfig cfg = config.get();
        CatchMomentum current = momentum.get(uuid);
        double rangeSquared = cfg.steadyCurrentRangeBlocks() * cfg.steadyCurrentRangeBlocks();

        boolean sameSpot = current != null
                && current.lastLocation().getWorld() != null
                && current.lastLocation().getWorld().equals(trigger.location().getWorld())
                && current.lastLocation().distanceSquared(trigger.location()) <= rangeSquared;

        if (sameSpot) {
            int newStacks = Math.min(current.stacks() + 1, cfg.steadyCurrentMaxStacks());
            momentum.put(uuid, new CatchMomentum(newStacks, trigger.location()));
            if (newStacks > current.stacks()) {
                player.sendActionBar(MINI.deserialize(
                        "<aqua>Steady Current: <white>Stack " + newStacks + "/"
                        + cfg.steadyCurrentMaxStacks() + " <gray>(-"
                        + (int) (newStacks * cfg.steadyCurrentStackReductionPercent())
                        + "% bite time)"));
            }
        } else {
            momentum.put(uuid, new CatchMomentum(1, trigger.location()));
        }
    }

    private double reductionFraction(UUID uuid) {
        FishingSkillConfig cfg = config.get();
        CatchMomentum m = momentum.get(uuid);
        if (m == null) return 0.0;
        return m.stacks() * (cfg.steadyCurrentStackReductionPercent() / 100.0);
    }
}
