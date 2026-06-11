package net.sylphian.minecraft.crates.gui.opening;

import net.sylphian.minecraft.crates.config.CrateConfig;
import net.sylphian.minecraft.crates.config.RewardEntry;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory holder for the rotation opening style.
 *
 * <p>Holds all pre-rolled rewards for the session and tracks which spin is currently
 * running. Stores the active animation task so it can be cancelled if the player
 * closes the GUI before all spins complete.</p>
 */
public class RotationGUIHolder implements InventoryHolder {

    private Inventory inventory;
    private final CrateConfig crate;
    private final List<RewardEntry> rolledRewards;
    private int currentSpin = 0;
    private boolean allSpinsComplete = false;
    private BukkitTask animationTask;

    public RotationGUIHolder(CrateConfig crate, List<RewardEntry> rolledRewards) {
        this.crate = crate;
        this.rolledRewards = new ArrayList<>(rolledRewards);
    }

    @Override
    public @NonNull Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public CrateConfig getCrate() { return crate; }
    public List<RewardEntry> getRolledRewards() { return rolledRewards; }
    public int getCurrentSpin() { return currentSpin; }
    public void incrementSpin() { currentSpin++; }
    public boolean hasMoreSpins() { return currentSpin < rolledRewards.size(); }
    public boolean isAllSpinsComplete() { return allSpinsComplete; }
    public void setAllSpinsComplete(boolean complete) { this.allSpinsComplete = complete; }

    public void setAnimationTask(BukkitTask task) { this.animationTask = task; }

    /** Cancels the active animation task if one is running. */
    public void cancelAnimation() {
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }
    }
}