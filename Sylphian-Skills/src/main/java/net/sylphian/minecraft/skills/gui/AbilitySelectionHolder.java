package net.sylphian.minecraft.skills.gui;

import net.sylphian.minecraft.skills.skill.ActiveAbility;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holder for the active-ability selection inventory.
 *
 * <p>Abilities are placed in a centered layout across a single 9-slot row.
 * Slot positions are computed at construction time and exposed via
 * {@link #getAbilityAt} for click handling.</p>
 */
public final class AbilitySelectionHolder implements InventoryHolder {

    private final Map<Integer, ActiveAbility> slotMap;
    private final UUID ownerUuid;

    /**
     * @param abilities the unlocked active abilities to display, in order
     * @param ownerUuid UUID of the player who opened this menu
     */
    public AbilitySelectionHolder(List<ActiveAbility> abilities, UUID ownerUuid) {
        this.ownerUuid = ownerUuid;

        Map<Integer, ActiveAbility> map = new LinkedHashMap<>();
        int startSlot = (9 - abilities.size()) / 2;
        for (int i = 0; i < abilities.size(); i++) {
            map.put(startSlot + i, abilities.get(i));
        }
        this.slotMap = Collections.unmodifiableMap(map);
    }

    /**
     * @param slot the raw inventory slot clicked
     * @return the ability at that slot, or {@code null} if the slot is empty
     */
    public @Nullable ActiveAbility getAbilityAt(int slot) {
        return slotMap.get(slot);
    }

    /**
     * @return a slot-to-ability mapping, used when populating the inventory
     */
    public Map<Integer, ActiveAbility> getSlotMap() {
        return slotMap;
    }

    /**
     * @return the UUID of the player who owns this menu
     */
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    @Override
    public @Nullable Inventory getInventory() {
        return null;
    }
}
