package net.sylphian.minecraft.fishing.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.db.models.FishEncyclopaediaModel;
import net.sylphian.minecraft.fishing.db.repositories.FishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.fish.LootEntry;
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

    private List<LootEntry> entries;
    private final FishEncyclopaediaRepository repository;
    private final JavaPlugin plugin;

    /**
     * Constructs a new EncyclopaediaMenu.
     *
     * @param entries all available fish in the plugin
     * @param repository  repository for player catch data
     * @param plugin      the plugin instance
     */
    public EncyclopaediaMenu(List<LootEntry> entries, FishEncyclopaediaRepository repository, JavaPlugin plugin) {
        this.entries = entries;
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
        int end = Math.min(start + PAGE_SIZE, entries.size());

        // Fill the inventory with fish entries for the current page
        for (int i = start; i < end; i++) {
            LootEntry entry = entries.get(i);

            // Check if the player has caught this entry before
            boolean unlocked = discovered.containsKey(entry.id());

            // Show entry info if unlocked, otherwise show as unknown
            inventory.setItem(i - start, unlocked ? createUnlockedFish(entry, discovered) : createUnknownFish());
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

    private ItemStack createUnlockedFish(LootEntry fish, Map<String, FishEncyclopaediaModel> discovered) {
        List<String> lore = new ArrayList<>(Arrays.asList(fish.description().split("\n")));

        FishEncyclopaediaModel model = discovered.get(fish.id());
        if (model != null) {
            lore.add("");
            lore.add("<gray>Caught: <white>" + model.timesCaught());
            lore.add("<gray>Biggest: <white>" + String.format("%.2f", model.biggestWeight()) + "kg");
        }

        if (fish.isGlobal()) {
            lore.add("");
            lore.add("<aqua>Biomes:");
            lore.add("<gray>• <white>All Biomes");
        } else if (!fish.biomes().isEmpty()) {
            lore.add("");
            lore.add("<aqua>Biomes:");
            for (Biome biome : fish.biomes()) {
                String formattedBiome = Arrays.stream(biome.getKey().getKey().split("_"))
                        .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                        .collect(Collectors.joining(" "));
                lore.add("<gray>• <white>" + formattedBiome);
            }
        }

        if (fish.hasYRestriction()) {
            lore.add("");
            lore.add("<aqua>Depth:");
            lore.add("<gray>• <white>" + formatYRestriction(fish));
        }

        if (fish.hasTimeRestriction()) {
            lore.add("");
            lore.add("<aqua>Active Time:");
            lore.add("<gray>• <white>" + formatTimeRestriction(fish));
        }

        return new ItemBuilder(fish.material())
                .name(fish.displayName())
                .loreStrings(lore)
                .build();
    }

    /**
     * Formats the Y restriction of a fish into a human-readable depth range string.
     *
     * @param fish the fish entry to format
     * @return a string such as "Below Y 60", "Above Y -64", or "Y -64 to 20"
     */
    private String formatYRestriction(LootEntry fish) {
        if (fish.minY() != null && fish.maxY() != null) {
            return "Y " + fish.minY() + " to " + fish.maxY();
        } else if (fish.minY() != null) {
            return "Above Y " + fish.minY();
        } else {
            return "Below Y " + fish.maxY();
        }
    }

    /**
     * Formats the time restriction of a fish into a human-readable clock range string.
     * Converts Minecraft ticks to approximate real-world time, where tick 0 = 6:00 AM.
     *
     * @param fish the fish entry to format
     * @return a string such as "18:00 – 06:00" or "After 12:00"
     */
    private String formatTimeRestriction(LootEntry fish) {
        if (fish.minTime() != null && fish.maxTime() != null) {
            return ticksToTime(fish.minTime()) + " – " + ticksToTime(fish.maxTime());
        } else if (fish.minTime() != null) {
            return "After " + ticksToTime(fish.minTime());
        } else {
            return "Before " + ticksToTime(fish.maxTime());
        }
    }

    /**
     * Converts a Minecraft world time in ticks to an approximate clock string.
     * Tick 0 = 6:00 AM, tick 6000 = 12:00 PM, tick 12000 = 6:00 PM, tick 18000 = 12:00 AM.
     *
     * @param ticks the world time in ticks (0–24000)
     * @return a formatted time string such as "18:00"
     */
    private String ticksToTime(long ticks) {
        int hour = (int) ((ticks / 1000 + 6) % 24);
        return String.format("%02d:00", hour);
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
                .lore("<gray>Discovered: <white>" + discovered + "/" + entries.size())
                .build();
    }

    private int getMaxPages() {
        return Math.max(
                0,
                (int) Math.ceil(
                        (double) entries.size()
                                / PAGE_SIZE
                ) - 1
        );
    }

    /**
     * Reloads the menu with an updated list of fish entries.
     *
     * @param fishEntries the updated list of all available fish
     */
    public void reload(List<LootEntry> fishEntries) {
        this.entries = fishEntries;
    }
}