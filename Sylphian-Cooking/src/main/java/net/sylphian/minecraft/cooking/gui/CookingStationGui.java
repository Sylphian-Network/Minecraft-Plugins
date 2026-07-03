package net.sylphian.minecraft.cooking.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.cooking.quality.CookingQuality;
import net.sylphian.minecraft.cooking.station.CookingStationState;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

/**
 * Builds and updates the custom cooking station inventory GUI.
 *
 * <p>Layout (4-row chest, 36 slots):</p>
 * <pre>
 *  Row 0: [X][X][I][I][I][I][I][X][X]   slots 2-6,  ingredient inputs
 *  Row 1: [X][X][O][O][O][O][O][X][X]   slots 11-15, output buffer (5 slots)
 *  Row 2: [X][X][X][X][P][X][X][X][X]   slot 22,    progress indicator
 *  Row 3: [X][X][X][X][F][X][X][X][X]   slot 31,    fuel input
 * </pre>
 */
public class CookingStationGui {

    /** Inventory slots that hold ingredients. */
    public static final int[] INGREDIENT_SLOTS = {2, 3, 4, 5, 6};

    /** Inventory slots that hold output items; take-only. */
    public static final int[] OUTPUT_SLOTS = {11, 12, 13, 14, 15};

    /** Slot index of the progress indicator (read-only). */
    public static final int PROGRESS_SLOT = 22;

    /** Slot index of the fuel input. */
    public static final int FUEL_SLOT = 31;

    /** PDC key stamped on placeholder glass pane items. */
    public static final NamespacedKey PLACEHOLDER_KEY =
            new NamespacedKey("sylphian-cooking", "placeholder");

    /** Slots the player can place items into: ingredients and fuel only. */
    private static final Set<Integer> PLACEABLE_SLOTS = Set.of(
            INGREDIENT_SLOTS[0], INGREDIENT_SLOTS[1], INGREDIENT_SLOTS[2],
            INGREDIENT_SLOTS[3], INGREDIENT_SLOTS[4],
            FUEL_SLOT
    );

    /** All slots the player may interact with: ingredients, outputs (take-only), and fuel. */
    private static final Set<Integer> INTERACTIVE_SLOTS = Set.of(
            INGREDIENT_SLOTS[0], INGREDIENT_SLOTS[1], INGREDIENT_SLOTS[2],
            INGREDIENT_SLOTS[3], INGREDIENT_SLOTS[4],
            OUTPUT_SLOTS[0], OUTPUT_SLOTS[1], OUTPUT_SLOTS[2],
            OUTPUT_SLOTS[3], OUTPUT_SLOTS[4],
            FUEL_SLOT
    );

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Component TITLE = MINI.deserialize("<dark_gray>Cooking Station");

    /**
     * Builds a fresh GUI inventory pre-populated from the given station state.
     *
     * @param state       the current station state
     * @param stationType the block material
     * @return the constructed inventory
     */
    public Inventory build(CookingStationState state, Material stationType) {
        CookingStationHolder holder = new CookingStationHolder(null);
        Inventory inv = Bukkit.createInventory(holder, 36, TITLE);
        holder.setInventory(inv);

        ItemStack filler = fillerPane();
        for (int i = 0; i < 36; i++) {
            if (!INTERACTIVE_SLOTS.contains(i) && i != PROGRESS_SLOT) {
                inv.setItem(i, filler);
            }
        }

        for (int i = 0; i < CookingStationState.INGREDIENT_COUNT; i++) {
            ItemStack ing = state.getIngredient(i);
            inv.setItem(INGREDIENT_SLOTS[i],
                    isPresent(ing) ? ing.clone() : ingredientPlaceholder());
        }

        for (int i = 0; i < CookingStationState.OUTPUT_COUNT; i++) {
            ItemStack out = state.getOutput(i);
            inv.setItem(OUTPUT_SLOTS[i],
                    isPresent(out) ? out.clone() : outputPlaceholder());
        }

        ItemStack fuel = state.getFuel();
        inv.setItem(FUEL_SLOT, isPresent(fuel) ? fuel.clone() : fuelPlaceholder());

        inv.setItem(PROGRESS_SLOT, progressItem(state));

        return inv;
    }

    /**
     * Refreshes the mutable display slots in an already-open inventory.
     *
     * @param inv   the open inventory to update
     * @param state the current station state
     */
    public void update(Inventory inv, CookingStationState state) {
        for (int i = 0; i < CookingStationState.INGREDIENT_COUNT; i++) {
            ItemStack ing = state.getIngredient(i);
            inv.setItem(INGREDIENT_SLOTS[i],
                    isPresent(ing) ? ing.clone() : ingredientPlaceholder());
        }

        for (int i = 0; i < CookingStationState.OUTPUT_COUNT; i++) {
            ItemStack out = state.getOutput(i);
            inv.setItem(OUTPUT_SLOTS[i],
                    isPresent(out) ? out.clone() : outputPlaceholder());
        }

        ItemStack fuel = state.getFuel();
        inv.setItem(FUEL_SLOT, isPresent(fuel) ? fuel.clone() : fuelPlaceholder());

        inv.setItem(PROGRESS_SLOT, progressItem(state));
    }

    /**
     * Returns the ingredient array index (0–4) for the given GUI slot, or -1 if not an ingredient slot.
     */
    public static int ingredientIndex(int guiSlot) {
        for (int i = 0; i < INGREDIENT_SLOTS.length; i++) {
            if (INGREDIENT_SLOTS[i] == guiSlot) return i;
        }
        return -1;
    }

    /**
     * Returns the output array index (0–4) for the given GUI slot, or -1 if not an output slot.
     */
    public static int outputIndex(int guiSlot) {
        for (int i = 0; i < OUTPUT_SLOTS.length; i++) {
            if (OUTPUT_SLOTS[i] == guiSlot) return i;
        }
        return -1;
    }

    /** Returns true if the slot can be interacted with (place or take). */
    public static boolean isInteractive(int guiSlot) {
        return INTERACTIVE_SLOTS.contains(guiSlot);
    }

    /** Returns true if items can be placed into the slot (ingredients and fuel only). */
    public static boolean isPlaceable(int guiSlot) {
        return PLACEABLE_SLOTS.contains(guiSlot);
    }

    /** Returns true if the slot is one of the five output slots. */
    public static boolean isOutputSlot(int guiSlot) {
        return outputIndex(guiSlot) >= 0;
    }

    /**
     * Builds the progress indicator item showing cook progress, fuel status,
     * and (when available) the quality tier of the last completed cycle.
     */
    private static ItemStack progressItem(CookingStationState state) {
        double fraction = state.cookProgressFraction();
        boolean cooking = state.getActiveRecipe() != null && state.getFuelRemaining() > 0;
        Material mat = cooking ? Material.ORANGE_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;

        int percent = (int) Math.round(fraction * 100);
        String bar = buildProgressBar(fraction, 10);

        String fuelLore;
        if (state.getFuelRemaining() > 0) {
            fuelLore = "<red>Fuel: " + formatTicks(state.getFuelRemaining());
        } else if (isPresent(state.getFuel())) {
            fuelLore = "<gray>Fuel: ready to ignite";
        } else {
            fuelLore = "<dark_gray>No fuel";
        }

        CookingQuality lastQuality = state.getLastQuality();
        if (lastQuality != null) {
            return new ItemBuilder(mat)
                    .name("<gold>Progress")
                    .lore("<yellow>" + bar + " " + percent + "%", fuelLore,
                            "<dark_gray>Last result: " + qualityLabel(lastQuality))
                    .build();
        }

        return new ItemBuilder(mat)
                .name("<gold>Progress")
                .lore("<yellow>" + bar + " " + percent + "%", fuelLore)
                .build();
    }

    private static String qualityLabel(CookingQuality quality) {
        return switch (quality) {
            case BURNT   -> "<dark_red>Burnt";
            case PLAIN   -> "<gray>Plain";
            case GOOD    -> "<green>Good";
            case PERFECT -> "<gold>Perfect";
        };
    }

    private static String formatTicks(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int remaining = seconds % 60;
        return minutes > 0 ? minutes + "m " + remaining + "s" : remaining + "s";
    }

    private static String buildProgressBar(double fraction, int length) {
        int filled = (int) Math.round(fraction * length);
        StringBuilder sb = new StringBuilder("<dark_gray>[");
        for (int i = 0; i < length; i++) {
            sb.append(i < filled ? "<gold>█" : "<gray>█");
        }
        sb.append("<dark_gray>]");
        return sb.toString();
    }

    private static ItemStack fillerPane() {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
    }

    private static ItemStack ingredientPlaceholder() {
        ItemStack item = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .name("<gray>Ingredient Slot")
                .lore("<dark_gray>Place an ingredient here")
                .build();
        item.editMeta(meta -> meta.getPersistentDataContainer()
                .set(PLACEHOLDER_KEY, PersistentDataType.BOOLEAN, true));
        return item;
    }

    private static ItemStack fuelPlaceholder() {
        ItemStack item = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray>Fuel")
                .lore("<dark_gray>Place fuel here", "<dark_gray>e.g. Coal, Wood")
                .build();
        item.editMeta(meta -> meta.getPersistentDataContainer()
                .set(PLACEHOLDER_KEY, PersistentDataType.BOOLEAN, true));
        return item;
    }

    private static ItemStack outputPlaceholder() {
        ItemStack item = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray>Output")
                .lore("<dark_gray>Cooked items will appear here")
                .build();
        item.editMeta(meta -> meta.getPersistentDataContainer()
                .set(PLACEHOLDER_KEY, PersistentDataType.BOOLEAN, true));
        return item;
    }

    public static boolean isPlaceholder(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
                .has(PLACEHOLDER_KEY, PersistentDataType.BOOLEAN);
    }

    private static boolean isPresent(ItemStack item) {
        return item != null && !item.getType().isAir();
    }
}
