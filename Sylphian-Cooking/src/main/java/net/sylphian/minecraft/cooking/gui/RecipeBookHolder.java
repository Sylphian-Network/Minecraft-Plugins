package net.sylphian.minecraft.cooking.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

/**
 * Inventory holder for the recipe book GUI.
 * Carries the menu instance and current page so the click listener can navigate.
 */
public final class RecipeBookHolder implements InventoryHolder {

    private final RecipeBookMenu menu;
    private final int page;

    /**
     * @param menu the recipe book menu instance
     * @param page the current page number (0-based)
     */
    public RecipeBookHolder(RecipeBookMenu menu, int page) {
        this.menu = menu;
        this.page = page;
    }

    /** @return the recipe book menu */
    public RecipeBookMenu getMenu() { return menu; }

    /** @return the current page number */
    public int getPage() { return page; }

    @Override
    @SuppressWarnings("NullableProblems")
    public @Nullable Inventory getInventory() { return null; }
}
