package net.sylphian.minecraft.fishing.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

/**
 * Inventory holder for the fish encyclopaedia GUI.
 * Stores the menu instance and current page number to facilitate navigation.
 */
public class EncyclopaediaHolder implements InventoryHolder {

    private final EncyclopaediaMenu menu;
    private final int page;

    /**
     * Constructs a new EncyclopaediaHolder.
     *
     * @param menu the menu instance
     * @param page the current page number
     */
    public EncyclopaediaHolder(EncyclopaediaMenu menu, int page) {
        this.menu = menu;
        this.page = page;
    }

    public EncyclopaediaMenu getMenu() {
        return menu;
    }

    public int getPage() {
        return page;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public @Nullable Inventory getInventory() {
        return null;
    }
}