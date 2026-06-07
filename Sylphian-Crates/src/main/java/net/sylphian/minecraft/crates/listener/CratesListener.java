package net.sylphian.minecraft.crates.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.crates.SylphianCrates;
import net.sylphian.minecraft.crates.config.CrateConfig;
import net.sylphian.minecraft.crates.config.KeyConfig;
import net.sylphian.minecraft.crates.config.RewardEntry;
import net.sylphian.minecraft.crates.gui.*;
import net.sylphian.minecraft.crates.key.CrateKey;
import net.sylphian.minecraft.crates.service.CrateService;
import net.sylphian.minecraft.core.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all inventory events for the crates and reward selection GUIs.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Detecting when a valid key is placed in the key slot and populating
 *       the crate slot accordingly.</li>
 *   <li>Consuming the key and triggering the crate opening when the crate slot
 *       is clicked with a valid key staged.</li>
 *   <li>Returning the key to the player if they close the GUI before opening.</li>
 *   <li>Handling reward selection when a player picks from rolled rewards.</li>
 *   <li>Preventing item movement in all non-interactive slots.</li>
 * </ul>
 */
public class CratesListener implements Listener {

    private final SylphianCrates plugin;
    private final CrateService crateService;

    /**
     * Constructs a new CratesListener.
     *
     * @param plugin       the plugin instance for config access and NBT key generation
     * @param crateService the service used to roll and grant rewards
     */
    public CratesListener(SylphianCrates plugin, CrateService crateService) {
        this.plugin = plugin;
        this.crateService = crateService;
    }

    /**
     * Handles all clicks within crates-managed inventories.
     *
     * @param event the click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getHolder() instanceof CratesGUIHolder holder) {
            handleCratesGUIClick(event, player, holder);
            return;
        }

        if (event.getInventory().getHolder() instanceof RewardSelectionGUIHolder holder) {
            handleSelectionClick(event, player, holder);
            return;
        }

        if (event.getInventory().getHolder() instanceof RotationGUIHolder) {
            event.setCancelled(true);
            return;
        }

        if (event.getInventory().getHolder() instanceof ColorfulGUIHolder colorfulHolder) {
            handleColorfulClick(event, player, colorfulHolder);
        }
    }

    /**
     * Prevents item dragging in all crates-managed inventories.
     *
     * @param event the drag event
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof CratesGUIHolder)
                && !(event.getInventory().getHolder() instanceof RewardSelectionGUIHolder)
                && !(event.getInventory().getHolder() instanceof RotationGUIHolder)
                && !(event.getInventory().getHolder() instanceof ColorfulGUIHolder)) return;

        int guiSize = event.getInventory().getSize();
        boolean touchesTopInventory = event.getRawSlots().stream().anyMatch(s -> s < guiSize);
        if (touchesTopInventory) event.setCancelled(true);
    }

    /**
     * Returns the staged key to the player if they close the GUI before opening.
     *
     * @param event the close event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        InventoryHolder invHolder = event.getInventory().getHolder();

        if (invHolder instanceof CratesGUIHolder cratesHolder) {
            KeyConfig stagedKey = cratesHolder.getStagedKey();
            if (stagedKey == null) return;
            ItemStack keyItem = CrateKey.create(stagedKey, plugin);
            player.getInventory().addItem(keyItem).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            cratesHolder.clearStaged();
            return;
        }

        if (invHolder instanceof RotationGUIHolder rotationHolder) {
            rotationHolder.cancelAnimation();
            if (!rotationHolder.isAllSpinsComplete()) {
                rotationHolder.getRolledRewards().forEach(r -> crateService.giveReward(player, r));
            }
        }
    }

    /**
     * Handles clicks within the main crates GUI.
     * Placing a key in the key slot stages it and populates the crate slot.
     * Clicking an empty key slot with a staged key returns it to the player.
     * Clicking the populated crate slot consumes the key and triggers the opening sequence.
     *
     * @param event  the click event
     * @param player the player who clicked
     * @param holder the GUI holder carrying staged key and crate state
     */
    private void handleCratesGUIClick(InventoryClickEvent event, Player player, CratesGUIHolder holder) {
        int slot = event.getRawSlot();

        if (slot >= event.getInventory().getSize()) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (slot == CratesGUI.KEY_SLOT) {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                if (holder.getStagedKey() != null) {
                    player.setItemOnCursor(CrateKey.create(holder.getStagedKey(), plugin));
                    CratesGUI.restoreKeySlot(event.getInventory());
                    CratesGUI.restoreCrateSlot(event.getInventory());
                    holder.clearStaged();
                }
                return;
            }

            if (holder.getStagedKey() != null) return;

            String keyId = CrateKey.getKeyId(cursor, plugin);
            if (keyId == null) return;

            KeyConfig keyConfig = plugin.getKeys().get(keyId);
            CrateConfig crateConfig = plugin.getCrates().get(keyConfig != null ? keyConfig.opens() : "");
            if (keyConfig == null || crateConfig == null) return;

            ItemStack toPlace = cursor.clone();
            toPlace.setAmount(1);

            holder.stage(keyConfig, crateConfig);
            event.getInventory().setItem(CratesGUI.KEY_SLOT, toPlace);
            populateCrateSlot(event.getInventory(), crateConfig);

            if (cursor.getAmount() > 1) {
                ItemStack remaining = cursor.clone();
                remaining.setAmount(cursor.getAmount() - 1);
                player.setItemOnCursor(remaining);
            } else {
                player.setItemOnCursor(null);
            }

            return;
        }

        if (slot == CratesGUI.CRATE_SLOT && holder.getStagedCrate() != null) {
            CrateConfig crate = holder.getStagedCrate();
            holder.clearStaged();
            CratesGUI.restoreKeySlot(event.getInventory());
            CratesGUI.restoreCrateSlot(event.getInventory());
            player.closeInventory();
            openCrate(player, crate);
        }
    }

    /**
     * Handles clicks within the reward selection GUI.
     * Clicking a reward slot toggles its selection state and refreshes its display name
     * and the confirm button to reflect the current selection count.
     * Clicking the confirm button when at least one reward is selected grants all
     * selected rewards to the player and closes the GUI. The player is not required
     * to fill all available picks before confirming.
     *
     * @param event  the click event
     * @param player the player who clicked
     * @param holder the GUI holder carrying the rolled rewards and current selection state
     */
    private void handleSelectionClick(InventoryClickEvent event, Player player,
                                      RewardSelectionGUIHolder holder) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        // Confirm button
        if (slot == holder.getConfirmSlot()) {
            if (!holder.getSelected().isEmpty()) {
                holder.getSelected().stream()
                        .map(i -> holder.getRewards().get(i))
                        .forEach(r -> crateService.giveReward(player, r));
                player.closeInventory();
            }
            return;
        }

        // Reward slot
        if (slot >= holder.getRewards().size()) return;

        boolean nowSelected = holder.toggleSelect(slot);
        ItemStack base = crateService.buildItem(holder.getRewards().get(slot));
        event.getInventory().setItem(slot, nowSelected ? RewardSelectionGUI.asSelected(base) : base);

        // Refresh confirm button
        event.getInventory().setItem(holder.getConfirmSlot(),
                RewardSelectionGUI.buildConfirmButton(holder.getSelected().size(), holder.getPicks()));
    }

    /**
     * Handles a pane click in the colorful opening GUI.
     * Each click rolls a reward from the pool, grants it immediately, and
     * reveals the reward item in the clicked slot.
     * When picks are exhausted, the GUI closes after a brief delay.
     *
     * @param event  the click event
     * @param player the player who clicked
     * @param colorfulHolder the GUI holder tracking slot assignments and remaining picks
     */
    private void handleColorfulClick(InventoryClickEvent event, Player player, ColorfulGUIHolder colorfulHolder) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        if (colorfulHolder.isRevealed(slot)) return;

        colorfulHolder.markRevealed(slot);
        colorfulHolder.decrementPicks();

        RewardEntry reward = crateService.rollOne(colorfulHolder.getCrate());
        event.getInventory().setItem(slot, crateService.buildItem(reward));
        crateService.giveReward(player, reward);

        if (colorfulHolder.isDone()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.closeInventory(), 20L);
        }
    }

    /**
     * Populates the crate slot with the crate's display item.
     * Lore includes all rewards from the pool with their percentage chances.
     *
     * @param inv   the GUI inventory
     * @param crate the crate config to display
     */
    private void populateCrateSlot(Inventory inv, CrateConfig crate) {
        double totalWeight = crate.pool().stream()
                .mapToDouble(RewardEntry::chance)
                .sum();

        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>Possible Rewards:");

        for (RewardEntry reward : crate.pool()) {
            double percent = (reward.chance() / totalWeight) * 100;
            String name = reward.externalItemId() != null
                    ? getDisplayName(crateService.buildItem(reward))
                    : reward.displayName();
            lore.add(String.format("<dark_aqua> • %s <gray>(%.1f%%)", name, percent));
        }

        ItemStack display = new ItemBuilder(crate.displayMaterial())
                .name(crate.displayName())
                .loreStrings(lore)
                .build();

        inv.setItem(CratesGUI.CRATE_SLOT, display);
    }

    /**
     * Opens the crate using the configured {@link net.sylphian.minecraft.crates.config.OpeningStyle}.
     * Rewards are rolled and presented differently depending on the style —
     * SELECTION uses a pick GUI, ROTATION plays a slot-machine animation,
     * and COLORFUL lets the player reveal panes for instant rewards.
     *
     * @param player the player opening the crate
     * @param crate  the crate being opened
     */
    private void openCrate(Player player, CrateConfig crate) {
        switch (crate.openingStyle()) {
            case SELECTION -> {
                List<RewardEntry> rolled = crateService.rollRewards(crate);
                if (crate.playerPicks() >= crate.totalRolls()) {
                    crateService.giveAll(player, rolled);
                } else {
                    RewardSelectionGUI.open(player, rolled, crate.playerPicks(), crateService);
                }
            }
            case ROTATION -> {
                List<RewardEntry> rolled = crateService.rollRewards(crate);
                RotationGUI.open(player, crate, rolled, crateService, plugin);
            }
            case COLORFUL -> ColorfulGUI.open(player, crate, crate.playerPicks());
        }
    }

    /**
     * Extracts the display name from an ItemStack's meta as a MiniMessage string.
     * Falls back to the item's translation key if no display name is set.
     *
     * @param item the item to read the name from
     * @return the MiniMessage display name string
     */
    private String getDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return MiniMessage.miniMessage().serialize(item.getItemMeta().displayName());
        }
        return "<white>" + item.getType().name();
    }
}