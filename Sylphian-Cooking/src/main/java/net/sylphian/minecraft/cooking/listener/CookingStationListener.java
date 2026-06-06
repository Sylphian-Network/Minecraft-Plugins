package net.sylphian.minecraft.cooking.listener;

import net.sylphian.minecraft.cooking.gui.CookingStationGui;
import net.sylphian.minecraft.cooking.gui.CookingStationHolder;
import net.sylphian.minecraft.cooking.station.CookingStationService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

/**
 * Handles all events related to cooking stations.
 *
 * <ul>
 *   <li>{@link PlayerInteractEvent} — intercepts right-clicks on furnaces and campfires
 *       to open the custom GUI instead of the vanilla interface.</li>
 *   <li>{@link InventoryClickEvent} — enforces slot rules (ingredient / fuel / output / locked)
 *       and propagates changes to the service.</li>
 *   <li>{@link InventoryDragEvent} — cancels drags that touch locked slots.</li>
 *   <li>{@link InventoryCloseEvent} — saves state and removes the player from viewers.</li>
 *   <li>{@link BlockBreakEvent} — drops all stored items and clears PDC.</li>
 *   <li>{@link BlockExplodeEvent} / {@link EntityExplodeEvent} — same as block break.</li>
 * </ul>
 */
public class CookingStationListener implements Listener {

    /** Block types that trigger the custom cooking GUI on right-click. */
    private static final Set<Material> STATION_BLOCKS = EnumSet.of(
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE
    );

    private final JavaPlugin plugin;
    private final CookingStationService service;

    /**
     * Constructs a new CookingStationListener.
     *
     * @param plugin  the owning plugin, used for scheduling deferred syncs
     * @param service the station service managing state and ticking
     */
    public CookingStationListener(JavaPlugin plugin, CookingStationService service) {
        this.plugin = plugin;
        this.service = service;
    }

    /**
     * Intercepts right-clicks on station blocks and opens the custom cooking GUI.
     * The vanilla furnace/campfire interaction is cancelled.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || !STATION_BLOCKS.contains(block.getType())) return;

        event.setCancelled(true);
        service.openStation(event.getPlayer(), block);
    }

    /**
     * Enforces slot rules when a player clicks inside the cooking station GUI.
     *
     * <ul>
     *   <li>Locked slots (glass filler, progress) — always cancelled.</li>
     *   <li>Output slot — only allow taking items, not placing.</li>
     *   <li>Fuel slot — only allow placing recognised fuels.</li>
     *   <li>Ingredient slots — allow placing and taking freely.</li>
     *   <li>Shift-clicks from player inventory — routed to the appropriate slot.</li>
     * </ul>
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof CookingStationHolder)) return;

        Inventory topInv = event.getInventory();
        int rawSlot = event.getRawSlot();
        int topSize = topInv.getSize();
        boolean clickedTop = rawSlot >= 0 && rawSlot < topSize;
        boolean clickedBottom = !clickedTop;

        if (clickedBottom && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            handleShiftClickFromPlayer(player, event.getCurrentItem(), topInv);
            return;
        }

        if (clickedTop && CookingStationGui.isEditable(rawSlot)) {
            ItemStack slotItem = topInv.getItem(rawSlot);
            if (CookingStationGui.isPlaceholder(slotItem)) {
                ItemStack cursor = event.getCursor();
                if (cursor == null || cursor.getType().isAir()) {
                    event.setCancelled(true);
                    return;
                }

                if (rawSlot == CookingStationGui.OUTPUT_SLOT) {
                    event.setCancelled(true);
                    return;
                }

                // Cancel to prevent the placeholder swapping onto the player's cursor
                event.setCancelled(true);

                int amount = event.getClick() == ClickType.RIGHT ? 1 : cursor.getAmount();
                ItemStack toPlace = cursor.clone();
                toPlace.setAmount(Math.min(amount, toPlace.getMaxStackSize()));
                topInv.setItem(rawSlot, toPlace);
                int remaining = cursor.getAmount() - toPlace.getAmount();
                event.getWhoClicked().setItemOnCursor(remaining > 0 ? cursor.asQuantity(remaining) : null);

                if (rawSlot == CookingStationGui.FUEL_SLOT) {
                    scheduleFuelSync(player, topInv);
                } else {
                    scheduleIngredientSync(player, rawSlot, topInv);
                }
                return;
            }
        }

        if (!clickedTop) return;

        if (!CookingStationGui.isEditable(rawSlot)) {
            event.setCancelled(true);
            return;
        }

        if (rawSlot == CookingStationGui.OUTPUT_SLOT) {
            handleOutputClick(event, player);
            return;
        }

        if (rawSlot == CookingStationGui.FUEL_SLOT) {
            handleFuelClick(event, player, topInv);
            return;
        }

        if (CookingStationGui.ingredientIndex(rawSlot) >= 0) {
            scheduleIngredientSync(player, rawSlot, topInv);
        }
    }

    /**
     * Cancels drag events that include any locked station slot.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof CookingStationHolder)) return;

        int topSize = event.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && !CookingStationGui.isEditable(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Saves station state to PDC and removes the player from the viewer list when they close the GUI.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof CookingStationHolder)) return;
        // Flush the inventory's current contents into the service state before saving to PDC.
        // This resolves the race condition where a deferred click-sync hasn't run yet
        // by the time the close event fires (both happen within the same server tick).
        service.syncFromInventory(player, event.getInventory());
        service.closeStation(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!STATION_BLOCKS.contains(event.getBlock().getType())) return;
        service.destroyStation(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().stream()
                .filter(b -> STATION_BLOCKS.contains(b.getType()))
                .forEach(service::destroyStation);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().stream()
                .filter(b -> STATION_BLOCKS.contains(b.getType()))
                .forEach(service::destroyStation);
    }

    private void handleOutputClick(InventoryClickEvent event, Player player) {
        if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
            event.setCancelled(true);
            return;
        }
        scheduleOutputSync(player);
    }

    private void handleFuelClick(InventoryClickEvent event, Player player, Inventory topInv) {
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir() && !service.isValidFuel(cursor)) {
            event.setCancelled(true);
            return;
        }
        scheduleFuelSync(player, topInv);
    }

    private void handleShiftClickFromPlayer(Player player, ItemStack item, Inventory topInv) {
        if (item == null || item.getType().isAir()) return;

        if (service.isValidFuel(item)) {
            ItemStack fuelSlot = topInv.getItem(CookingStationGui.FUEL_SLOT);
            if (fuelSlot == null || fuelSlot.getType().isAir() || CookingStationGui.isPlaceholder(fuelSlot)) {
                topInv.setItem(CookingStationGui.FUEL_SLOT, item.clone());
                item.setAmount(0);
                scheduleFuelSync(player, topInv);
                return;
            }
        }

        for (int guiSlot : CookingStationGui.INGREDIENT_SLOTS) {
            ItemStack existing = topInv.getItem(guiSlot);
            if (existing == null || existing.getType().isAir() || CookingStationGui.isPlaceholder(existing)) {
                ItemStack toPlace = item.clone();
                topInv.setItem(guiSlot, toPlace);
                item.setAmount(0);
                service.setIngredient(player, CookingStationGui.ingredientIndex(guiSlot), toPlace);
                return;
            }
        }
    }

    /** Schedules a 1-tick delayed sync of the output slot back to the service. */
    private void scheduleOutputSync(Player player) {
        player.getServer().getScheduler().runTask(plugin, () -> service.clearOutput(player));
    }

    /** Schedules a 1-tick delayed sync of the fuel slot back to the service. */
    private void scheduleFuelSync(Player player, Inventory topInv) {
        player.getServer().getScheduler().runTask(
                plugin,
                () -> {
                    ItemStack updated = topInv.getItem(CookingStationGui.FUEL_SLOT);
                    service.setFuel(player,
                            (updated != null && !updated.getType().isAir() && !CookingStationGui.isPlaceholder(updated))
                                    ? updated.clone() : null);
                }
        );
    }

    /** Schedules a 1-tick delayed sync of the given ingredient slot back to the service. */
    private void scheduleIngredientSync(Player player, int guiSlot, Inventory topInv) {
        int ingIdx = CookingStationGui.ingredientIndex(guiSlot);
        if (ingIdx < 0) return;

        player.getServer().getScheduler().runTask(
                plugin,
                () -> {
                    ItemStack updated = topInv.getItem(guiSlot);
                    service.setIngredient(player, ingIdx,
                            (updated != null && !updated.getType().isAir() && !CookingStationGui.isPlaceholder(updated))
                                    ? updated.clone() : null);
                }
        );
    }
}
