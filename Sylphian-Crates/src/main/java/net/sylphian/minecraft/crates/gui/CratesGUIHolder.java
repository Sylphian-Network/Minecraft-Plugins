package net.sylphian.minecraft.crates.gui;

import net.sylphian.minecraft.crates.config.CrateConfig;
import net.sylphian.minecraft.crates.config.KeyConfig;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nullable;

/**
 * Custom {@link InventoryHolder} for the crates GUI.
 *
 * <p>Holds references to the key and crate currently staged in the GUI,
 * allowing the event listener to identify crate inventories and read
 * their state without external maps.</p>
 */
public class CratesGUIHolder implements InventoryHolder {

    private Inventory inventory;
    private KeyConfig stagedKey;
    private CrateConfig stagedCrate;

    /**
     * Returns the key currently placed in the key slot, or null if none.
     *
     * @return the staged KeyConfig, or null
     */
    @Nullable
    public KeyConfig getStagedKey() { return stagedKey; }

    /**
     * Returns the crate populated in the crate slot, or null if none.
     *
     * @return the staged CrateConfig, or null
     */
    @Nullable
    public CrateConfig getStagedCrate() { return stagedCrate; }

    /**
     * Stages a key and its associated crate in the GUI.
     *
     * @param key   the key placed by the player
     * @param crate the crate the key maps to
     */
    public void stage(KeyConfig key, CrateConfig crate) {
        this.stagedKey = key;
        this.stagedCrate = crate;
    }

    /** Clears the staged key and crate. */
    public void clearStaged() {
        this.stagedKey = null;
        this.stagedCrate = null;
    }

    @Override
    public Inventory getInventory() { return inventory; }

    /**
     * Sets the backing inventory for this holder.
     * Called immediately after the inventory is created.
     *
     * @param inventory the inventory to back this holder
     */
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
}