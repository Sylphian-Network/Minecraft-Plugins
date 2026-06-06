package net.sylphian.minecraft.cooking.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.cooking.station.CookingStationState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import net.sylphian.minecraft.core.util.ItemBuilder;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

/**
 * Builds and updates the custom cooking station inventory GUI.
 */
public class CookingStationGui {

    /** Inventory slots that hold ingredients (internal indices into the 27-slot chest). */
    public static final int[] INGREDIENT_SLOTS = {1, 2, 3, 4, 5};

    /** Slot index of the fuel input. */
    public static final int FUEL_SLOT = 22;

    /** Slot index of the progress indicator (read-only). */
    public static final int PROGRESS_SLOT = 16;

    /** Slot index of the recipe output (take-only). */
    public static final int OUTPUT_SLOT = 8;

    /** Slot index of the arrow separator between ingredient slots and the output (read-only). */
    public static final int SEPARATOR_SLOT = 6;

    /**
     * PDC key stamped on all placeholder glass pane items.
     * Used to distinguish placeholders from real items placed by players.
     */
    public static final NamespacedKey PLACEHOLDER_KEY = new NamespacedKey("sylphian-cooking", "placeholder");

    /** All editable slot indices (ingredients, fuel, output). */
    private static final Set<Integer> EDITABLE_SLOTS = Set.of(
            INGREDIENT_SLOTS[0], INGREDIENT_SLOTS[1], INGREDIENT_SLOTS[2],
            INGREDIENT_SLOTS[3], INGREDIENT_SLOTS[4],
            FUEL_SLOT, OUTPUT_SLOT
    );

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Component TITLE = MINI.deserialize("<dark_gray>Cooking Station");

    /**
     * Builds a fresh GUI inventory pre-populated from the given station state.
     *
     * @param state       the current station state
     * @param stationType the Material of the block (used to locate it on close)
     * @return the constructed inventory
     */
    public Inventory build(CookingStationState state, Material stationType) {
        CookingStationHolder holder = new CookingStationHolder(null);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);
        holder.setInventory(inv);

        ItemStack filler = fillerPane();
        for (int i = 0; i < 27; i++) {
            if (!EDITABLE_SLOTS.contains(i) && i != PROGRESS_SLOT && i != SEPARATOR_SLOT) {
                inv.setItem(i, filler);
            }
        }

        inv.setItem(SEPARATOR_SLOT, separatorItem());

        for (int i = 0; i < CookingStationState.INGREDIENT_COUNT; i++) {
            ItemStack ing = state.getIngredient(i);
            inv.setItem(INGREDIENT_SLOTS[i], (ing != null && !ing.getType().isAir()) ? ing.clone() : ingredientPlaceholder());
        }

        ItemStack fuel = state.getFuel();
        inv.setItem(FUEL_SLOT, (fuel != null && !fuel.getType().isAir()) ? fuel.clone() : fuelPlaceholder());

        ItemStack output = state.getOutput();
        inv.setItem(OUTPUT_SLOT, (output != null && !output.getType().isAir()) ? output.clone() : outputPlaceholder());

        inv.setItem(PROGRESS_SLOT, progressItem(state));

        return inv;
    }

    /**
     * Refreshes only the mutable display slots (output, fuel, ingredients, progress)
     * without rebuilding the entire inventory. Safe to call from the tick loop.
     *
     * @param inv   the open inventory to update
     * @param state the current station state
     */
    public void update(Inventory inv, CookingStationState state) {
        for (int i = 0; i < CookingStationState.INGREDIENT_COUNT; i++) {
            ItemStack ing = state.getIngredient(i);
            inv.setItem(INGREDIENT_SLOTS[i], (ing != null && !ing.getType().isAir()) ? ing.clone() : ingredientPlaceholder());
        }

        ItemStack fuel = state.getFuel();
        inv.setItem(FUEL_SLOT, (fuel != null && !fuel.getType().isAir()) ? fuel.clone() : fuelPlaceholder());

        ItemStack output = state.getOutput();
        inv.setItem(OUTPUT_SLOT, (output != null && !output.getType().isAir()) ? output.clone() : outputPlaceholder());

        inv.setItem(PROGRESS_SLOT, progressItem(state));
    }

    /**
     * Returns the ingredient slot index (0–4) for the given GUI slot, or -1 if not an ingredient slot.
     *
     * @param guiSlot the inventory slot index
     * @return ingredient array index (0–4), or -1
     */
    public static int ingredientIndex(int guiSlot) {
        for (int i = 0; i < INGREDIENT_SLOTS.length; i++) {
            if (INGREDIENT_SLOTS[i] == guiSlot) return i;
        }
        return -1;
    }

    /**
     * Returns true if items can be freely placed into and taken from this slot.
     * The output slot is editable for taking but placement is handled separately.
     */
    public static boolean isEditable(int guiSlot) {
        return EDITABLE_SLOTS.contains(guiSlot);
    }

    private static ItemStack fillerPane() {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
    }

    /**
     * Builds the progress indicator item. Shows a flame / arrow symbol and
     * current progress as a percentage in the lore.
     *
     * @param state the station state to read progress from
     * @return the progress ItemStack
     */
    private static ItemStack progressItem(CookingStationState state) {
        double fraction = state.cookProgressFraction();
        boolean cooking = state.getActiveRecipe() != null && state.getFuelRemaining() > 0;
        Material mat = cooking ? Material.ORANGE_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;

        int percent = (int) Math.round(fraction * 100);
        String bar = buildProgressBar(fraction, 10);

        String fuelLore;
        if (state.getFuelRemaining() > 0) {
            fuelLore = "<red>Fuel: " + state.getFuelRemaining() + " ticks";
        } else if (state.getFuel() != null && !state.getFuel().getType().isAir()) {
            fuelLore = "<gray>Fuel: ready to ignite";
        } else {
            fuelLore = "<dark_gray>No fuel";
        }

        return new ItemBuilder(mat)
                .name("<gold>Progress")
                .lore("<yellow>" + bar + " " + percent + "%", fuelLore)
                .build();
    }

    /** Builds a Unicode block progress bar of the given length. */
    private static String buildProgressBar(double fraction, int length) {
        int filled = (int) Math.round(fraction * length);
        StringBuilder sb = new StringBuilder("<dark_gray>[");
        for (int i = 0; i < length; i++) {
            sb.append(i < filled ? "<gold>█" : "<gray>█");
        }
        sb.append("<dark_gray>]");
        return sb.toString();
    }

    private static ItemStack separatorItem() {
        return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).name("<gray>▶").build();
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
}
