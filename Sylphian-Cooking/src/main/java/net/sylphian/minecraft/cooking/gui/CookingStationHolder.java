package net.sylphian.minecraft.cooking.gui;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marks a custom inventory as belonging to a cooking station at a specific block location.
 * Used by the event listener to distinguish cooking station GUIs from other inventories.
 */
public class CookingStationHolder implements InventoryHolder {

    private final Location stationLocation;
    private Inventory inventory;

    /**
     * Constructs a holder tied to the given block location.
     *
     * @param stationLocation the location of the furnace or campfire block
     */
    public CookingStationHolder(Location stationLocation) {
        this.stationLocation = stationLocation;
    }

    /** Returns the block location this GUI belongs to. */
    public Location getStationLocation() {
        return stationLocation;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /** Called by {@link CookingStationGui} after creating the inventory. */
    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
