package net.sylphian.minecraft.skills.gui;

import net.sylphian.minecraft.skills.skill.Skill;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

/**
 * Inventory holder for the per-skill ability detail GUI.
 *
 * <p>Stores the parent menu instance (used to navigate back to the category
 * list) and the skill whose abilities are being displayed.</p>
 */
public class SkillDetailHolder implements InventoryHolder {

    /** Raw slot index of the back button in the detail view. */
    public static final int BACK_SLOT = 48;

    private final SkillsMenu menu;
    private final Skill skill;

    /**
     * @param menu  the parent skills menu (used for back navigation)
     * @param skill the skill whose abilities are shown in this inventory
     */
    public SkillDetailHolder(SkillsMenu menu, Skill skill) {
        this.menu = menu;
        this.skill = skill;
    }

    /**
     * @return the parent menu, used to re-open the category list on back
     */
    public SkillsMenu getMenu() {
        return menu;
    }

    /**
     * @return the skill displayed in this detail view
     */
    public Skill getSkill() {
        return skill;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public @Nullable Inventory getInventory() {
        return null;
    }
}
