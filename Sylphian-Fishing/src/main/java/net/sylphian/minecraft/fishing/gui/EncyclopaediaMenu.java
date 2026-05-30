package net.sylphian.minecraft.fishing.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.db.models.FishEncyclopaediaModel;
import net.sylphian.minecraft.fishing.db.repositories.FishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.fish.FishEntry;
import net.sylphian.minecraft.fishing.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the generation and display of the fish encyclopaedia GUI.
 * Handles pagination, discovering which fish the player has caught,
 * and building the inventory with appropriate items and lore.
 */
public class EncyclopaediaMenu {

    private static final int PAGE_SIZE = 45;

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final List<FishEntry> fishEntries;
    private final FishEncyclopaediaRepository repository;
    private final JavaPlugin plugin;

    /**
     * Constructs a new EncyclopaediaMenu.
     *
     * @param fishEntries all available fish in the plugin
     * @param repository  repository for player catch data
     * @param plugin      the plugin instance
     */
    public EncyclopaediaMenu(List<FishEntry> fishEntries, FishEncyclopaediaRepository repository, JavaPlugin plugin) {
        this.fishEntries = fishEntries;
        this.repository = repository;
        this.plugin = plugin;
    }

    /**
     * Opens the encyclopaedia GUI for a player on a specific page.
     * Fetches player catch data from the database asynchronously.
     *
     * @param player the player to open the menu for
     * @param page   the page number to open
     */
    public void open(Player player, int page) {
        page = Math.clamp(page, 0, getMaxPages());
        int finalPage = page;

        repository.findAllForPlayer(player.getUniqueId())
                .thenAccept(models -> {

                    Map<String, FishEncyclopaediaModel> discovered =
                            models.stream()
                                    .collect(Collectors.toMap(
                                            FishEncyclopaediaModel::fishId,
                                            m -> m
                                    ));

                    Bukkit.getScheduler().runTask(plugin, () -> createInventory(player, finalPage, discovered));
                });
    }

    /**
     * Builds and opens the inventory for the player.
     *
     * @param player     the player
     * @param page       the page number
     * @param discovered map of fish IDs to catch data models
     */
    private void createInventory(Player player, int page, Map<String, FishEncyclopaediaModel> discovered) {
        Inventory inventory = Bukkit.createInventory(
                new EncyclopaediaHolder(this, page),
                54,
                MINI.deserialize(
                        "<aqua>Fishing Encyclopaedia"
                )
        );

        fillBottomRow(inventory);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, fishEntries.size());

        // Fill the inventory with fish entries for the current page
        for (int i = start; i < end; i++) {
            FishEntry fish = fishEntries.get(i);

            // Check if the player has caught this fish before
            boolean unlocked = discovered.containsKey(fish.getId());

            // Show fish info if unlocked, otherwise show as unknown
            inventory.setItem(i - start, unlocked ? createUnlockedFish(fish, discovered) : createUnknownFish());
        }

        // Navigation and info items
        inventory.setItem(48, createPreviousButton(page));
        inventory.setItem(49, createInfoItem(discovered.size()));
        inventory.setItem(50, createNextButton(page));

        player.openInventory(inventory);
    }

    private void fillBottomRow(Inventory inventory) {
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.empty())
                .build();

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack createUnknownFish() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray>Data not available yet")
                .lore(
                        "<dark_gray>Catch this fish to",
                        "<dark_gray>unlock its information."
                )
                .build();
    }

    private ItemStack createUnlockedFish(FishEntry fish, Map<String, FishEncyclopaediaModel> discovered) {
        List<String> lore = new ArrayList<>();
        lore.addAll(Arrays.asList(fish.getDescription().split("\n")));

        FishEncyclopaediaModel model = discovered.get(fish.getId());

        if (model != null) {
            lore.add("");
            lore.add("<gray>Caught: <white>" + model.timesCaught());
            lore.add("<gray>Biggest: <white>" + String.format("%.2f", model.biggestWeight()) + "kg");
        }

        if (fish.isGlobal()) {
            lore.add("");
            lore.add("<aqua>Biomes:");
            lore.add("<gray>• <white>All Biomes");

        } else if (!fish.getBiomes().isEmpty()) {
            lore.add("");
            lore.add("<aqua>Biomes:");

            for (Biome biome : fish.getBiomes()) {
                String formattedBiome = Arrays.stream(biome.getKey().getKey().split("_"))
                        .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                        .collect(Collectors.joining(" "));

                lore.add("<gray>• <white>" + formattedBiome);
            }
        }

        return new ItemBuilder(fish.getMaterial())
                .name(fish.getDisplayName())
                .loreStrings(lore)
                .build();
    }

    private ItemStack createPreviousButton(int page) {
        ItemBuilder builder = new ItemBuilder(Material.ARROW)
                .name("<yellow>Previous Page");

        if (page <= 0) {
            builder.lore("<gray>No previous page.");
        }

        return builder.build();
    }

    private ItemStack createNextButton(int page) {
        ItemBuilder builder = new ItemBuilder(Material.ARROW)
                .name("<yellow>Next Page");

        if (page >= getMaxPages()) {
            builder.lore("<gray>No next page.");
        }

        return builder.build();
    }

    private ItemStack createInfoItem(int discovered) {
        return new ItemBuilder(Material.BOOK)
                .name("<aqua>Fishing Progress")
                .lore("<gray>Discovered: <white>" + discovered + "/" + fishEntries.size())
                .build();
    }

    private int getMaxPages() {
        return Math.max(
                0,
                (int) Math.ceil(
                        (double) fishEntries.size()
                                / PAGE_SIZE
                ) - 1
        );
    }
}