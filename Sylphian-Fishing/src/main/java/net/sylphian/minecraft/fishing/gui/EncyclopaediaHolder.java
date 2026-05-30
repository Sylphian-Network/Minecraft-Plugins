package net.sylphian.minecraft.fishing.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

public class EncyclopaediaHolder implements InventoryHolder {

    private final EncyclopaediaMenu menu;
    private final int page;

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