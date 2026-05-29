package net.sylphian.minecraft.fishing.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

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
    public Inventory getInventory() {
        return null;
    }
}