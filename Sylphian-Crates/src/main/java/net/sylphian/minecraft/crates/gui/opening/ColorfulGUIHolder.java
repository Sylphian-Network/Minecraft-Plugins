package net.sylphian.minecraft.crates.gui.opening;

import net.sylphian.minecraft.crates.config.CrateConfig;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Inventory holder for the colorful pane-picker opening style.
 *
 * <p>Tracks how many picks the player has remaining and which slots have
 * already been revealed to prevent double-clicking.</p>
 */
public class ColorfulGUIHolder implements InventoryHolder {

    private Inventory inventory;
    private final CrateConfig crate;
    private int picksRemaining;
    private final Set<Integer> revealedSlots = new HashSet<>();

    public ColorfulGUIHolder(CrateConfig crate, int picks) {
        this.crate = crate;
        this.picksRemaining = picks;
    }

    @Override
    public @NonNull Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public CrateConfig getCrate() { return crate; }
    public int getPicksRemaining() { return picksRemaining; }
    public void decrementPicks() { picksRemaining--; }
    public boolean isRevealed(int slot) { return revealedSlots.contains(slot); }
    public void markRevealed(int slot) { revealedSlots.add(slot); }
    public boolean isDone() { return picksRemaining <= 0; }
}