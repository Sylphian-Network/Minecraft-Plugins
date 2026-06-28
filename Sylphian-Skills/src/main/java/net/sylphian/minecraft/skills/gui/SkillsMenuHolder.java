package net.sylphian.minecraft.skills.gui;

import net.sylphian.minecraft.skills.skill.Skill;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Inventory holder for the skills category list GUI.
 *
 * <p>Stores the menu instance and the ordered list of skills used to build the
 * inventory, so the click listener can resolve a clicked slot back to a skill
 * without relying on item metadata or title matching.</p>
 */
public class SkillsMenuHolder implements InventoryHolder {

    private final SkillsMenu menu;
    private final List<Skill> skills;

    /**
     * @param menu   the menu instance (used to open the detail view on click)
     * @param skills the skills in the order they were placed into the inventory,
     *               where index equals slot number
     */
    public SkillsMenuHolder(SkillsMenu menu, List<Skill> skills) {
        this.menu = menu;
        this.skills = skills;
    }

    /**
     * @return the menu that opened this inventory
     */
    public SkillsMenu getMenu() {
        return menu;
    }

    /**
     * Returns the skill placed at the given raw slot, or {@code null} if the slot
     * holds a decorative item or is out of range.
     *
     * @param slot the raw slot index from the click event
     * @return the skill at that slot, or {@code null}
     */
    public @Nullable Skill getSkillAt(int slot) {
        if (slot < 0 || slot >= skills.size()) return null;
        return skills.get(slot);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public @Nullable Inventory getInventory() {
        return null;
    }
}
