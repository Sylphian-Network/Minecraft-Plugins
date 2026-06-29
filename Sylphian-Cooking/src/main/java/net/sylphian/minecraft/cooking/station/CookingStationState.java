package net.sylphian.minecraft.cooking.station;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable runtime state for a single cooking station (furnace or campfire block).
 *
 * <p>All state is persisted to the block's {@link org.bukkit.persistence.PersistentDataContainer}
 * via {@link CookingStationPdc} whenever the station is closed or the tick loop
 * detects a state change.</p>
 *
 * <p>Slot layout (internal indices, not GUI slot indices):</p>
 * <ul>
 *   <li>0–4 — ingredient slots</li>
 *   <li>5 — fuel slot</li>
 *   <li>6 — output slot</li>
 * </ul>
 */
public class CookingStationState {

    /** Number of ingredient slots in a cooking station. */
    public static final int INGREDIENT_COUNT = 5;

    private final ItemStack[] ingredients = new ItemStack[INGREDIENT_COUNT];
    private ItemStack fuel;
    private ItemStack output;

    /** Ticks of progress toward completing the active recipe. */
    private int cookProgress;

    /** Ticks of fuel burn time remaining before the next fuel item must be consumed. */
    private int fuelRemaining;

    /** The recipe currently being cooked, or null if no recipe matches the current ingredients. */
    private CookingRecipe activeRecipe;

    /**
     * The player who last mutated an ingredient or fuel slot, used to attribute
     * skill effects (cook time reduction, bonus yield, XP) to the correct player.
     * Null if no player has interacted with this station in the current session.
     */
    private UUID lastInteractor;

    /**
     * The effective cook time in ticks for the current recipe cycle, set by the
     * skills framework on the first tick of each new recipe to apply any passive
     * cook time reductions. Zero until a cook cycle begins.
     */
    private int effectiveCookTime;

    /** UUIDs of players currently viewing this station's GUI. */
    private final Set<UUID> viewers = new HashSet<>();

    /**
     * Returns the item in the given ingredient slot.
     *
     * @param index 0–4
     * @return the item, or null if the slot is empty
     */
    public ItemStack getIngredient(int index) {
        return ingredients[index];
    }

    /**
     * Sets the item in the given ingredient slot.
     *
     * @param index 0–4
     * @param item  the item to place, or null to clear
     */
    public void setIngredient(int index, ItemStack item) {
        ingredients[index] = item;
    }

    /**
     * Returns a snapshot copy of all ingredient slots.
     * Modifying the returned array does not affect this state.
     *
     * @return array of length {@link #INGREDIENT_COUNT}
     */
    public ItemStack[] ingredientSnapshot() {
        ItemStack[] copy = new ItemStack[INGREDIENT_COUNT];
        for (int i = 0; i < INGREDIENT_COUNT; i++) {
            copy[i] = ingredients[i] != null ? ingredients[i].clone() : null;
        }
        return copy;
    }

    public ItemStack getFuel() { return fuel; }
    public void setFuel(ItemStack fuel) { this.fuel = fuel; }

    public ItemStack getOutput() { return output; }
    public void setOutput(ItemStack output) { this.output = output; }

    public int getCookProgress() { return cookProgress; }
    public void setCookProgress(int cookProgress) { this.cookProgress = cookProgress; }

    public int getFuelRemaining() { return fuelRemaining; }
    public void setFuelRemaining(int fuelRemaining) { this.fuelRemaining = fuelRemaining; }

    public CookingRecipe getActiveRecipe() { return activeRecipe; }
    public void setActiveRecipe(CookingRecipe activeRecipe) { this.activeRecipe = activeRecipe; }

    /** @return the UUID of the player who last mutated an ingredient or fuel slot, or null */
    public UUID getLastInteractor() { return lastInteractor; }

    /**
     * Records the player who last mutated an ingredient or fuel slot.
     *
     * @param lastInteractor the interacting player's UUID
     */
    public void setLastInteractor(UUID lastInteractor) { this.lastInteractor = lastInteractor; }

    /**
     * Returns the effective cook time in ticks for the current recipe cycle.
     * Zero means no cycle is active; use {@link CookingRecipe#cookTime()} as the base.
     *
     * @return effective cook time, or 0 if not yet set for the current cycle
     */
    public int getEffectiveCookTime() { return effectiveCookTime; }

    /**
     * Sets the effective cook time for the current recipe cycle.
     * Set to 0 when the active recipe changes to signal that the next tick
     * should re-derive it (giving skills a chance to apply reductions).
     *
     * @param effectiveCookTime the cook time in ticks, or 0 to reset
     */
    public void setEffectiveCookTime(int effectiveCookTime) { this.effectiveCookTime = effectiveCookTime; }

    public Set<UUID> getViewers() { return viewers; }
    public void addViewer(UUID uuid) { viewers.add(uuid); }
    public void removeViewer(UUID uuid) { viewers.remove(uuid); }

    /**
     * Returns true if every ingredient slot, the fuel slot, and the output slot are all empty.
     */
    public boolean isEmpty() {
        for (ItemStack ingredient : ingredients) {
            if (ingredient != null && !ingredient.getType().isAir()) return false;
        }
        if (fuel != null && !fuel.getType().isAir()) return false;
        if (output != null && !output.getType().isAir()) return false;
        return true;
    }

    /**
     * Returns the fraction of cook progress as a value between 0.0 and 1.0.
     * Uses {@link #effectiveCookTime} when set, falling back to the recipe's base cook time.
     * Returns 0.0 if no recipe is active.
     */
    public double cookProgressFraction() {
        if (activeRecipe == null) return 0.0;
        int total = effectiveCookTime > 0 ? effectiveCookTime : activeRecipe.cookTime();
        if (total <= 0) return 0.0;
        return Math.min(1.0, (double) cookProgress / total);
    }
}
