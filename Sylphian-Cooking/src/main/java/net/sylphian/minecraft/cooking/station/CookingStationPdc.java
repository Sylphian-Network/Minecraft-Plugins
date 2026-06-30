package net.sylphian.minecraft.cooking.station;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Serializes {@link CookingStationState} to and from a block entity's {@link PersistentDataContainer}
 * under the {@code sylphian-cooking} namespace. Items use Paper's byte serialization; an empty byte
 * array marks an absent slot.
 */
public final class CookingStationPdc {

    private static final String NS = "sylphian-cooking";

    private static final NamespacedKey[] INGREDIENT_KEYS = new NamespacedKey[CookingStationState.INGREDIENT_COUNT];

    private static final NamespacedKey[] OUTPUT_KEYS = new NamespacedKey[CookingStationState.OUTPUT_COUNT];

    private static final NamespacedKey KEY_FUEL           = new NamespacedKey(NS, "fuel");
    private static final NamespacedKey KEY_COOK_PROGRESS  = new NamespacedKey(NS, "cook_progress");
    private static final NamespacedKey KEY_FUEL_REMAINING = new NamespacedKey(NS, "fuel_remaining");

    static {
        for (int i = 0; i < CookingStationState.INGREDIENT_COUNT; i++) {
            INGREDIENT_KEYS[i] = new NamespacedKey(NS, "ingredient_" + i);
        }
        for (int i = 0; i < CookingStationState.OUTPUT_COUNT; i++) {
            OUTPUT_KEYS[i] = new NamespacedKey(NS, "output_" + i);
        }
    }

    private CookingStationPdc() {}

    /**
     * Returns true if the given block has any cooking station data stored in its PDC.
     *
     * @param block the block to check; must be a {@link TileState}
     * @return true if cooking station PDC data exists
     */
    public static boolean hasCookingData(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof TileState tileState)) return false;
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        return pdc.has(KEY_COOK_PROGRESS, PersistentDataType.INTEGER)
                || pdc.has(INGREDIENT_KEYS[0], PersistentDataType.BYTE_ARRAY);
    }

    /**
     * Serializes the given {@link CookingStationState} into the block entity's PDC.
     * Call this whenever station state needs to be persisted (on GUI close, on shutdown).
     *
     * @param block        the furnace or campfire block
     * @param cookingState the state to persist
     */
    public static void save(Block block, CookingStationState cookingState) {
        BlockState blockState = block.getState();
        if (!(blockState instanceof TileState tileState)) return;

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();

        for (int i = 0; i < CookingStationState.INGREDIENT_COUNT; i++) {
            writeItem(pdc, INGREDIENT_KEYS[i], cookingState.getIngredient(i));
        }

        writeItem(pdc, KEY_FUEL, cookingState.getFuel());

        for (int i = 0; i < CookingStationState.OUTPUT_COUNT; i++) {
            writeItem(pdc, OUTPUT_KEYS[i], cookingState.getOutput(i));
        }

        pdc.set(KEY_COOK_PROGRESS,  PersistentDataType.INTEGER, cookingState.getCookProgress());
        pdc.set(KEY_FUEL_REMAINING, PersistentDataType.INTEGER, cookingState.getFuelRemaining());

        tileState.update();
    }

    /**
     * Loads a {@link CookingStationState} from the block entity's PDC.
     * Returns a fresh empty state if no data is found.
     *
     * @param block the furnace or campfire block
     * @return the loaded state (never null)
     */
    public static CookingStationState load(Block block) {
        CookingStationState cookingState = new CookingStationState();
        BlockState blockState = block.getState();
        if (!(blockState instanceof TileState tileState)) return cookingState;

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();

        for (int i = 0; i < CookingStationState.INGREDIENT_COUNT; i++) {
            cookingState.setIngredient(i, readItem(pdc, INGREDIENT_KEYS[i]));
        }

        cookingState.setFuel(readItem(pdc, KEY_FUEL));

        for (int i = 0; i < CookingStationState.OUTPUT_COUNT; i++) {
            cookingState.setOutput(i, readItem(pdc, OUTPUT_KEYS[i]));
        }

        cookingState.setCookProgress(pdc.getOrDefault(KEY_COOK_PROGRESS,  PersistentDataType.INTEGER, 0));
        cookingState.setFuelRemaining(pdc.getOrDefault(KEY_FUEL_REMAINING, PersistentDataType.INTEGER, 0));

        return cookingState;
    }

    /**
     * Removes all cooking station PDC keys from the block entity.
     * Call this when the station is cleared or broken.
     *
     * @param block the furnace or campfire block
     */
    public static void clear(Block block) {
        BlockState blockState = block.getState();
        if (!(blockState instanceof TileState tileState)) return;

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        for (NamespacedKey key : INGREDIENT_KEYS) pdc.remove(key);
        pdc.remove(KEY_FUEL);
        for (NamespacedKey key : OUTPUT_KEYS) pdc.remove(key);
        pdc.remove(KEY_COOK_PROGRESS);
        pdc.remove(KEY_FUEL_REMAINING);

        tileState.update();
    }

    private static void writeItem(PersistentDataContainer pdc, NamespacedKey key, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            pdc.set(key, PersistentDataType.BYTE_ARRAY, new byte[0]);
        } else {
            pdc.set(key, PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
        }
    }

    private static ItemStack readItem(PersistentDataContainer pdc, NamespacedKey key) {
        byte[] bytes = pdc.get(key, PersistentDataType.BYTE_ARRAY);
        if (bytes == null || bytes.length == 0) return null;
        try {
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
