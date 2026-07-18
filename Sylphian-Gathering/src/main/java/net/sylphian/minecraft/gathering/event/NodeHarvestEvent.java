package net.sylphian.minecraft.gathering.event;

import net.sylphian.minecraft.gathering.node.LootEntry;
import net.sylphian.minecraft.gathering.world.LiveNode;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired on the main thread from inside a harvest, after the skill-level gate and
 * before the loot roll. Skills mutate the accumulators on this event and the
 * engine applies them when the event returns uncancelled.
 *
 * <p>{@link #getYieldMultiplier()} and {@link #getXpMultiplier()} are meant to be
 * multiplied into, never assigned, so stacked passives compose:
 * {@code e.setYieldMultiplier(e.getYieldMultiplier() * 2.0)}.</p>
 */
public class NodeHarvestEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final LiveNode node;

    private double yieldMultiplier = 1.0;
    private double xpMultiplier = 1.0;
    private final List<LootEntry> bonusLoot = new ArrayList<>();
    private int respawnSecondsOverride = -1;
    private boolean deplete = true;
    private boolean cancelled;

    public NodeHarvestEvent(Player player, LiveNode node) {
        this.player = player;
        this.node = node;
    }

    /** @return the harvesting player */
    public Player player() {
        return player;
    }

    /** @return the node being harvested */
    public LiveNode node() {
        return node;
    }

    /** @return the accumulated yield multiplier applied to base loot amounts */
    public double getYieldMultiplier() {
        return yieldMultiplier;
    }

    /** @param yieldMultiplier the new yield multiplier; abilities should multiply into the current value */
    public void setYieldMultiplier(double yieldMultiplier) {
        this.yieldMultiplier = yieldMultiplier;
    }

    /** @return the accumulated XP multiplier applied to the node's base XP */
    public double getXpMultiplier() {
        return xpMultiplier;
    }

    /** @param xpMultiplier the new XP multiplier; abilities should multiply into the current value */
    public void setXpMultiplier(double xpMultiplier) {
        this.xpMultiplier = xpMultiplier;
    }

    /** @return the mutable list of extra loot entries granted in addition to the base and modifier loot */
    public List<LootEntry> getBonusLoot() {
        return bonusLoot;
    }

    /**
     * Adds one extra loot entry to grant on this harvest.
     *
     * @param entry the bonus loot entry
     */
    public void addBonusLoot(LootEntry entry) {
        bonusLoot.add(entry);
    }

    /** @return the respawn override in seconds, or a value below 1 to use the node/config default */
    public int getRespawnSecondsOverride() {
        return respawnSecondsOverride;
    }

    /** @param respawnSecondsOverride seconds before respawn; a value below 1 defers to the node/config default */
    public void setRespawnSecondsOverride(int respawnSecondsOverride) {
        this.respawnSecondsOverride = respawnSecondsOverride;
    }

    /** @return whether the node depletes after this harvest; false leaves it available */
    public boolean isDeplete() {
        return deplete;
    }

    /** @param deplete false to leave the node available after the harvest */
    public void setDeplete(boolean deplete) {
        this.deplete = deplete;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
