package net.sylphian.minecraft.crates.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.crates.config.RewardEntry;
import net.sylphian.minecraft.crates.service.CrateService;
import net.sylphian.minecraft.crates.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GUI shown when a player must pick from multiple rolled rewards.
 *
 * <p>Displayed when {@link net.sylphian.minecraft.crates.config.CrateConfig#playerPicks()}
 * is less than {@link net.sylphian.minecraft.crates.config.CrateConfig#totalRolls()}.
 * Rolled rewards are shown as clickable items the player can toggle selected or deselected.
 * Once the required number of rewards are selected, the player confirms via the bottom-row
 * confirm button to receive their chosen rewards.</p>
 */
public class RewardSelectionGUI {

    private RewardSelectionGUI() {}

    /**
     * Opens the reward selection GUI for the given player.
     *
     * @param player       the player choosing their rewards
     * @param rewards      the rewards rolled from the crate
     * @param picks        how many rewards the player may select
     * @param crateService the service used to build reward display items
     */
    public static void open(Player player, List<RewardEntry> rewards, int picks, CrateService crateService) {
        int rewardRows = (int) Math.ceil(rewards.size() / 9.0);
        int size = (rewardRows + 1) * 9; // extra row for confirm button

        RewardSelectionGUIHolder holder = new RewardSelectionGUIHolder(rewards, picks);
        Inventory inv = Bukkit.createInventory(holder, size,
                MiniMessage.miniMessage().deserialize("<dark_gray>Choose Your Rewards"));
        holder.setInventory(inv);

        for (int i = 0; i < rewards.size(); i++) {
            inv.setItem(i, crateService.buildItem(rewards.get(i)));
        }

        // Fill bottom row with gray glass panes
        ItemStack filler = new ItemBuilder(org.bukkit.Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray> ")
                .build();
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(holder.getConfirmSlot(), buildConfirmButton(0, picks));
        player.openInventory(inv);
    }

    /**
     * Builds the confirm button item reflecting the current selection progress.
     * The button uses a lime dye when at least one reward is selected and a gray dye
     * when nothing is selected. The player may confirm with fewer than the maximum picks.
     *
     * @param selected the number of rewards currently selected
     * @param picks    the total number of rewards the player must select
     * @return the confirm button {@link ItemStack}
     */
    public static ItemStack buildConfirmButton(int selected, int picks) {
        boolean ready = selected > 0;
        Material mat = ready ? Material.LIME_DYE : Material.GRAY_DYE;
        String name = ready
                ? "<green><bold>Confirm Selection <reset><dark_gray>(" + selected + "/" + picks + ")"
                : "<gray>Select a reward to confirm";
        return new ItemBuilder(mat)
                .name(name)
                .lore("<dark_gray>You may confirm with fewer than " + picks + " picks")
                .build();
    }

    /**
     * Returns a copy of the given item with a green {@code ✔} prefix on its display name
     * to indicate it has been selected by the player.
     *
     * @param base the original reward item to mark as selected
     * @return a copy of the item with the selection indicator applied to its name
     */
    public static ItemStack asSelected(ItemStack base) {
        ItemStack copy = base.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return copy;

        Component originalName = meta.hasDisplayName()
                ? Objects.requireNonNull(meta.displayName())
                : Component.translatable(copy.getType().translationKey());

        meta.displayName(
                Component.text("✔ ")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(originalName.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
        );

        copy.setItemMeta(meta);
        return copy;
    }
}