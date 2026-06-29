package net.sylphian.minecraft.cooking.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.cooking.config.CookingConfig;
import net.sylphian.minecraft.cooking.mastery.MasteryAccessor;
import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import net.sylphian.minecraft.cooking.recipe.IngredientSpec;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds and opens the paginated recipe book GUI.
 *
 * <p>Layout (6-row chest, 54 slots):</p>
 * <pre>
 *   Rows 0-4 (slots 0-44): recipe entries, PAGE_SIZE per page
 *   Row 5   (slots 45-53): filler, with prev/info/next at slots 48/49/50
 * </pre>
 *
 * <p>A recipe is considered locked if the player has never cooked it (cook count = 0).
 * Locked entries show as a gray glass pane named {@code ???} with only the ingredient
 * list in the lore. Unlocked entries display the recipe output item with cook time,
 * ingredients, and mastery progress appended to its lore.</p>
 */
public final class RecipeBookMenu {

    private static final int PAGE_SIZE = 45;
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private List<CookingRecipe> recipes;
    private volatile CookingConfig config;
    private final MasteryAccessor masteryAccessor;

    /**
     * @param recipes         all loaded cooking recipes
     * @param config          core cooking config, used for the mastery threshold
     * @param masteryAccessor sync accessor for per-player cook counts
     */
    public RecipeBookMenu(List<CookingRecipe> recipes, CookingConfig config, MasteryAccessor masteryAccessor) {
        this.recipes = List.copyOf(recipes);
        this.config = config;
        this.masteryAccessor = masteryAccessor;
    }

    /**
     * Opens the recipe book at the given page for the player.
     * Reads mastery counts from the in-memory cache; no async required.
     *
     * @param player the player to open the book for
     * @param page   the page number to display (clamped to valid range)
     */
    public void open(Player player, int page) {
        page = Math.clamp(page, 0, maxPage());

        Inventory inv = Bukkit.createInventory(
                new RecipeBookHolder(this, page),
                54,
                MINI.deserialize("<dark_gray>Recipe Book")
        );

        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        UUID uuid = player.getUniqueId();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, recipes.size());
        for (int i = start; i < end; i++) {
            CookingRecipe recipe = recipes.get(i);
            int cookCount = masteryAccessor.getCount(uuid, recipe.id());
            inv.setItem(i - start, cookCount > 0 ? unlockedEntry(recipe, cookCount) : lockedEntry(recipe));
        }

        inv.setItem(48, prevButton(page));
        inv.setItem(49, infoItem(player));
        inv.setItem(50, nextButton(page));

        player.openInventory(inv);
    }

    /**
     * Updates the recipe list and config after a plugin reload.
     *
     * @param recipes the reloaded recipe list
     * @param config  the reloaded core config
     */
    public void reload(List<CookingRecipe> recipes, CookingConfig config) {
        this.recipes = List.copyOf(recipes);
        this.config = config;
    }

    private ItemStack lockedEntry(CookingRecipe recipe) {
        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>Cook this recipe to unlock it.");
        lore.add("");
        lore.add("<gray>Ingredients:");
        lore.addAll(ingredientLines(recipe));
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray>???")
                .loreStrings(lore)
                .build();
    }

    private ItemStack unlockedEntry(CookingRecipe recipe, int cookCount) {
        CookingConfig cfg = config;
        int threshold = cfg.masteryThreshold();

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Cook time: <white>" + formatTicks(recipe.cookTime()));
        lore.add("");
        lore.add("<gray>Ingredients:");
        lore.addAll(ingredientLines(recipe));
        lore.add("");
        if (cookCount >= threshold) {
            lore.add("<gold>★ Mastered");
        } else {
            lore.add("<gray>Mastery: <white>" + cookCount + "<dark_gray>/<white>" + threshold);
        }

        ItemStack output = recipe.output();
        ItemBuilder builder = new ItemBuilder(output.getType()).loreStrings(lore);
        if (output.hasItemMeta() && output.getItemMeta().hasDisplayName()) {
            builder.name(Objects.requireNonNull(output.getItemMeta().displayName()));
        }
        return builder.build();
    }

    private List<String> ingredientLines(CookingRecipe recipe) {
        Map<String, Long> grouped = recipe.ingredients().stream()
                .collect(Collectors.groupingBy(IngredientSpec::displayId, LinkedHashMap::new, Collectors.counting()));
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            String name = formatIngredientId(entry.getKey());
            long count = entry.getValue();
            lines.add("  <dark_gray>- <white>" + (count > 1 ? count + "x " : "") + name);
        }
        return lines;
    }

    /**
     * Converts a raw ingredient display ID to a human-readable name.
     * Strips the namespace prefix from namespaced IDs, then title-cases
     * the remainder with underscores and slashes treated as word separators.
     */
    private static String formatIngredientId(String displayId) {
        String id = displayId.contains(":") ? displayId.substring(displayId.indexOf(':') + 1) : displayId;
        id = id.replace('_', ' ').replace('/', ' ');
        return Arrays.stream(id.split(" "))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static String formatTicks(int ticks) {
        int seconds = ticks / 20;
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int rem = seconds % 60;
            return rem > 0 ? minutes + "m " + rem + "s" : minutes + "m";
        }
        return seconds + "s";
    }

    private ItemStack infoItem(Player player) {
        UUID uuid = player.getUniqueId();
        long discovered = recipes.stream()
                .filter(r -> masteryAccessor.getCount(uuid, r.id()) > 0)
                .count();
        return new ItemBuilder(Material.BOOK)
                .name("<dark_gray>Recipe Book")
                .lore("<gray>Discovered: <white>" + discovered + "<dark_gray>/<white>" + recipes.size())
                .build();
    }

    private ItemStack prevButton(int page) {
        ItemBuilder builder = new ItemBuilder(Material.ARROW).name("<yellow>Previous Page");
        if (page <= 0) builder.lore("<gray>No previous page.");
        return builder.build();
    }

    private ItemStack nextButton(int page) {
        ItemBuilder builder = new ItemBuilder(Material.ARROW).name("<yellow>Next Page");
        if (page >= maxPage()) builder.lore("<gray>No next page.");
        return builder.build();
    }

    private int maxPage() {
        return Math.max(0, (int) Math.ceil((double) recipes.size() / PAGE_SIZE) - 1);
    }
}
