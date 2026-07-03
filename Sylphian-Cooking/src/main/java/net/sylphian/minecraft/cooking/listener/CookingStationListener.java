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
 * Handles all events related to cooking stations: GUI open, inventory interaction,
 * drag validation, close persistence, and block destruction.
 */
public class CookingStationListener implements Listener {

    /** Block types treated as cooking stations. */
    public static final Set<Material> STATION_BLOCKS = EnumSet.of(
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE
    );

    private final JavaPlugin plugin;
    private final CookingStationService service;

    public CookingStationListener(JavaPlugin plugin, CookingStationService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || !STATION_BLOCKS.contains(block.getType())) return;

        event.setCancelled(true);
        service.openStation(event.getPlayer(), block);
    }

    /** Enforces slot rules and propagates changes to the service on every click in the station GUI. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof CookingStationHolder)) return;

        Inventory topInv = event.getInventory();
        int rawSlot = event.getRawSlot();
        int topSize = topInv.getSize();
        boolean clickedTop    = rawSlot >= 0 && rawSlot < topSize;
        boolean clickedBottom = !clickedTop;

        // Shift-click from player inventory → route to ingredient or fuel slot.
        if (clickedBottom && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            handleShiftClickFromPlayer(player, event.getCurrentItem(), topInv);
            return;
        }

        if (!clickedTop) return;

        // Locked / display slots: block entirely.
        if (!CookingStationGui.isInteractive(rawSlot)) {
            event.setCancelled(true);
            return;
        }

        // Output slots: allow taking, block placing.
        if (CookingStationGui.isOutputSlot(rawSlot)) {
            handleOutputClick(event, player, topInv);
            return;
        }

        // Ingredient or fuel slots: handle placeholder replacement and validate fuel.
        ItemStack slotItem = topInv.getItem(rawSlot);
        if (CookingStationGui.isPlaceholder(slotItem)) {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                event.setCancelled(true);
                return;
            }

            if (rawSlot == CookingStationGui.FUEL_SLOT && !service.isValidFuel(cursor)) {
                event.setCancelled(true);
                return;
            }

            // Cancel to prevent the placeholder swapping onto the cursor.
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

        // Non-placeholder fuel or ingredient slot: sync after the click resolves.
        if (rawSlot == CookingStationGui.FUEL_SLOT) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir() && !service.isValidFuel(cursor)) {
                event.setCancelled(true);
                return;
            }
            scheduleFuelSync(player, topInv);
        } else if (CookingStationGui.ingredientIndex(rawSlot) >= 0) {
            scheduleIngredientSync(player, rawSlot, topInv);
        }
    }

    /** Cancels any drag that touches a non-placeable slot (output, progress, or filler). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof CookingStationHolder)) return;

        int topSize = event.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && !CookingStationGui.isPlaceable(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof CookingStationHolder)) return;
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

    private void handleOutputClick(InventoryClickEvent event, Player player, Inventory topInv) {
        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        int outIdx = CookingStationGui.outputIndex(rawSlot);
        if (outIdx < 0) return;

        ItemStack slotItem = topInv.getItem(rawSlot);
        if (slotItem == null || slotItem.getType().isAir() || CookingStationGui.isPlaceholder(slotItem)) {
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) return;

        int total = slotItem.getAmount();

        switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY -> {
                ItemStack taken = service.takeOutput(player, outIdx, total);
                if (taken != null) giveToInventory(player, taken);
            }
            case HOTBAR_SWAP -> {
                ItemStack taken = service.takeOutput(player, outIdx, total);
                if (taken != null) giveToHotbarOrInventory(player, event.getHotbarButton(), taken);
            }
            case DROP_ONE_SLOT -> {
                ItemStack taken = service.takeOutput(player, outIdx, 1);
                if (taken != null) dropAtPlayer(player, taken);
            }
            case DROP_ALL_SLOT -> {
                ItemStack taken = service.takeOutput(player, outIdx, total);
                if (taken != null) dropAtPlayer(player, taken);
            }
            case PICKUP_HALF -> {
                ItemStack taken = service.takeOutput(player, outIdx, (total + 1) / 2);
                if (taken != null) player.setItemOnCursor(taken);
            }
            default -> {
                ItemStack taken = service.takeOutput(player, outIdx, total);
                if (taken != null) player.setItemOnCursor(taken);
            }
        }
    }

    /** Adds an item to the player's inventory, dropping any overflow at their feet. */
    private void giveToInventory(Player player, ItemStack item) {
        for (ItemStack overflow : player.getInventory().addItem(item).values()) {
            dropAtPlayer(player, overflow);
        }
    }

    /** Places an item into the given hotbar slot if empty, otherwise adds it to the inventory. */
    private void giveToHotbarOrInventory(Player player, int hotbarButton, ItemStack item) {
        if (hotbarButton >= 0 && hotbarButton <= 8) {
            ItemStack existing = player.getInventory().getItem(hotbarButton);
            if (existing == null || existing.getType().isAir()) {
                player.getInventory().setItem(hotbarButton, item);
                return;
            }
        }
        giveToInventory(player, item);
    }

    /** Drops an item naturally at the player's location. */
    private void dropAtPlayer(Player player, ItemStack item) {
        player.getWorld().dropItemNaturally(player.getLocation(), item);
    }

    private void handleShiftClickFromPlayer(Player player, ItemStack item, Inventory topInv) {
        if (item == null || item.getType().isAir()) return;

        // Try fuel slot first if the item is a valid fuel.
        if (service.isValidFuel(item)) {
            ItemStack fuelSlot = topInv.getItem(CookingStationGui.FUEL_SLOT);
            if (fuelSlot == null || fuelSlot.getType().isAir() || CookingStationGui.isPlaceholder(fuelSlot)) {
                topInv.setItem(CookingStationGui.FUEL_SLOT, item.clone());
                item.setAmount(0);
                scheduleFuelSync(player, topInv);
                return;
            }
        }

        // Route to the first available ingredient slot.
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

    /** Schedules a 1-tick delayed sync of the fuel slot back to the service. */
    private void scheduleFuelSync(Player player, Inventory topInv) {
        player.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack updated = topInv.getItem(CookingStationGui.FUEL_SLOT);
            service.setFuel(player,
                    (updated != null && !updated.getType().isAir() && !CookingStationGui.isPlaceholder(updated))
                            ? updated.clone() : null);
        });
    }

    /** Schedules a 1-tick delayed sync of the given ingredient slot back to the service. */
    private void scheduleIngredientSync(Player player, int guiSlot, Inventory topInv) {
        int ingIdx = CookingStationGui.ingredientIndex(guiSlot);
        if (ingIdx < 0) return;
        player.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack updated = topInv.getItem(guiSlot);
            service.setIngredient(player, ingIdx,
                    (updated != null && !updated.getType().isAir() && !CookingStationGui.isPlaceholder(updated))
                            ? updated.clone() : null);
        });
    }
}
