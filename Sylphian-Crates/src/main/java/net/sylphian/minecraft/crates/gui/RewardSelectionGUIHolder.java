package net.sylphian.minecraft.crates.gui;

import net.sylphian.minecraft.crates.config.RewardEntry;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Holder used to identify reward selection inventories in events
 * and carry the rolled rewards for click handling.
 */
public class RewardSelectionGUIHolder implements InventoryHolder {

    private Inventory inventory;
    private final List<RewardEntry> rewards;
    private final int picks;
    private final LinkedHashSet<Integer> selected = new LinkedHashSet<>();

    /**
     * Constructs a new RewardSelectionGUIHolder.
     *
     * @param rewards the rolled rewards to display for selection
     * @param picks   how many rewards the player may select
     */
    public RewardSelectionGUIHolder(List<RewardEntry> rewards, int picks) {
        this.rewards = rewards;
        this.picks = picks;
    }

    /**
     * Returns the rolled rewards available for selection.
     *
     * @return the list of rolled rewards
     */
    public List<RewardEntry> getRewards() { return rewards; }

    /**
     * Returns the total number of rewards the player may select.
     *
     * @return the pick limit
     */
    public int getPicks() { return picks; }

    /**
     * Returns the set of currently selected reward indices, in insertion order.
     *
     * @return the selected reward indices
     */
    public LinkedHashSet<Integer> getSelected() { return selected; }

    /**
     * Toggles the selection state of the reward at the given index.
     * If the reward is already selected, it is deselected. If it is not selected
     * and the pick limit has not been reached, it is added to the selection.
     *
     * @param index the reward slot index to toggle
     * @return {@code true} if the reward is now selected, {@code false} if deselected or the pick limit was already reached
     */
    public boolean toggleSelect(int index) {
        if (selected.contains(index)) {
            selected.remove(index);
            return false;
        }
        if (selected.size() < picks) {
            selected.add(index);
            return true;
        }
        return false; // at cap, do nothing
    }

    /**
     * Returns the slot index of the confirm button, positioned at the centre of the bottom row.
     *
     * @return the confirm button slot index
     */
    public int getConfirmSlot() { return inventory.getSize() - 5; }

    @Override
    public @NonNull Inventory getInventory() { return inventory; }

    /**
     * Sets the backing inventory for this holder.
     *
     * @param inventory the inventory to back this holder
     */
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
}
