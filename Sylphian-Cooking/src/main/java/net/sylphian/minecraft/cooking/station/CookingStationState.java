package net.sylphian.minecraft.cooking.station;

import net.sylphian.minecraft.cooking.quality.CookingQuality;
import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable runtime state for a single cooking station (furnace or campfire block).
 * Persisted to the block's PDC via {@link CookingStationPdc} on GUI close and state changes.
 */
public class CookingStationState {

    /** Number of ingredient slots in a cooking station. */
    public static final int INGREDIENT_COUNT = 5;

    /** Number of output slots in a cooking station. */
    public static final int OUTPUT_COUNT = 5;

    private final ItemStack[] ingredients = new ItemStack[INGREDIENT_COUNT];
    private final ItemStack[] outputs     = new ItemStack[OUTPUT_COUNT];
    private ItemStack fuel;

    /** Ticks of progress toward completing the active recipe. */
    private int cookProgress;

    /** Ticks of fuel burn time remaining before the next fuel item must be consumed. */
    private int fuelRemaining;

    /** The recipe currently being cooked, or null if no recipe matches the current ingredients. */
    private CookingRecipe activeRecipe;

    /** UUID of the player who last mutated an ingredient or fuel slot. Null until first interaction this session. */
    private UUID lastInteractor;

    /** Cook time in ticks for the current cycle after passive reductions. Zero until set on the first tick of a cycle. */
    private int effectiveCookTime;

    /** Quality tier of the last completed cycle. Not persisted to PDC. Null if no cycle has completed. */
    private @Nullable CookingQuality lastQuality;

    /** When true, the next completed quality cook is forced to Perfect (Perfect Sear). Not persisted to PDC. */
    private boolean forceNextPerfect;

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

    /**
     * Returns the item in the given output slot.
     *
     * @param index 0–4
     * @return the item, or null if the slot is empty
     */
    public @Nullable ItemStack getOutput(int index) {
        return outputs[index];
    }

    /**
     * Sets the item in the given output slot.
     *
     * @param index 0–4
     * @param item  the item to place, or null to clear
     */
    public void setOutput(int index, @Nullable ItemStack item) {
        outputs[index] = item;
    }

    public @Nullable ItemStack getFuel() { return fuel; }
    public void setFuel(@Nullable ItemStack fuel) { this.fuel = fuel; }

    public int getCookProgress() { return cookProgress; }
    public void setCookProgress(int cookProgress) { this.cookProgress = cookProgress; }

    public int getFuelRemaining() { return fuelRemaining; }
    public void setFuelRemaining(int fuelRemaining) { this.fuelRemaining = fuelRemaining; }

    public @Nullable CookingRecipe getActiveRecipe() { return activeRecipe; }
    public void setActiveRecipe(@Nullable CookingRecipe activeRecipe) { this.activeRecipe = activeRecipe; }

    /** @return the UUID of the player who last mutated an ingredient or fuel slot, or null */
    public @Nullable UUID getLastInteractor() { return lastInteractor; }

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
     *
     * @param effectiveCookTime the cook time in ticks, or 0 to reset for the next cycle
     */
    public void setEffectiveCookTime(int effectiveCookTime) { this.effectiveCookTime = effectiveCookTime; }

    /**
     * Returns the quality tier of the most recently completed cook cycle, or null if none.
     * Not persisted to PDC.
     */
    public @Nullable CookingQuality getLastQuality() { return lastQuality; }

    /** @param lastQuality the rolled quality, or null to clear */
    public void setLastQuality(@Nullable CookingQuality lastQuality) { this.lastQuality = lastQuality; }

    /** @return true if the next quality cook is forced to Perfect */
    public boolean isForceNextPerfect() { return forceNextPerfect; }

    /** @param forceNextPerfect true to force the next quality cook to Perfect */
    public void setForceNextPerfect(boolean forceNextPerfect) { this.forceNextPerfect = forceNextPerfect; }

    public Set<UUID> getViewers() { return viewers; }
    public void addViewer(UUID uuid) { viewers.add(uuid); }
    public void removeViewer(UUID uuid) { viewers.remove(uuid); }

    /**
     * Returns true if every ingredient slot, the fuel slot, and all output slots are empty.
     */
    public boolean isEmpty() {
        for (ItemStack ingredient : ingredients) {
            if (ingredient != null && !ingredient.getType().isAir()) return false;
        }
        if (fuel != null && !fuel.getType().isAir()) return false;
        for (ItemStack output : outputs) {
            if (output != null && !output.getType().isAir()) return false;
        }
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
