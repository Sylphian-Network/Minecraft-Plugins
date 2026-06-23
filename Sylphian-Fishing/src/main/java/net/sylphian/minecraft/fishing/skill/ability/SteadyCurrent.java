package net.sylphian.minecraft.fishing.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.skills.skill.Ability;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Passive perk unlocked at level 20.
 *
 * <p>Players who catch fish consecutively within a small radius build up
 * stacks of Steady Current, each reducing bite wait time further. Moving
 * too far between catches resets the streak to one stack.</p>
 */
public final class SteadyCurrent implements Ability {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    @Override public String id()           { return "fishing:steady-current"; }
    @Override public String name()         { return "Steady Current"; }
    @Override public String description()  {
        return "Catching fish in the same spot builds stacks, each reducing bite time by 5% (max 5 stacks).";
    }
    @Override public int    unlockLevel()  { return 20; }

    /**
     * Updates a player's momentum after a catch. Increments stacks if the catch
     * is within range of the previous one, or resets to 1 stack otherwise.
     *
     * @param player       the catching player
     * @param uuid         the player's UUID
     * @param catchLocation location of this catch
     * @param cfg          current config snapshot
     * @param momentum     the shared momentum state map
     */
    public void updateMomentum(Player player, UUID uuid, Location catchLocation,
                               FishingSkillConfig cfg, Map<UUID, CatchMomentum> momentum) {
        CatchMomentum current = momentum.get(uuid);
        double rangeSquared = cfg.steadyCurrentRangeBlocks() * cfg.steadyCurrentRangeBlocks();

        boolean sameSpot = current != null
                && current.lastLocation().getWorld() != null
                && current.lastLocation().getWorld().equals(catchLocation.getWorld())
                && current.lastLocation().distanceSquared(catchLocation) <= rangeSquared;

        if (sameSpot) {
            int newStacks = Math.min(current.stacks() + 1, cfg.steadyCurrentMaxStacks());
            momentum.put(uuid, new CatchMomentum(newStacks, catchLocation));
            if (newStacks > current.stacks()) {
                player.sendActionBar(MINI.deserialize(
                        "<aqua>Steady Current: <white>Stack " + newStacks + "/"
                        + cfg.steadyCurrentMaxStacks() + " <gray>(-"
                        + (int) (newStacks * cfg.steadyCurrentStackReductionPercent())
                        + "% bite time)"));
            }
        } else {
            momentum.put(uuid, new CatchMomentum(1, catchLocation));
        }
    }

    /**
     * @param uuid     the player's UUID
     * @param cfg      current config snapshot
     * @param momentum the shared momentum state map
     * @return the fractional wait-time reduction from current stacks (e.g. 0.10 for 2 stacks at 5% each)
     */
    public double reductionFraction(UUID uuid, FishingSkillConfig cfg, Map<UUID, CatchMomentum> momentum) {
        CatchMomentum m = momentum.get(uuid);
        if (m == null) return 0.0;
        return m.stacks() * (cfg.steadyCurrentStackReductionPercent() / 100.0);
    }
}
