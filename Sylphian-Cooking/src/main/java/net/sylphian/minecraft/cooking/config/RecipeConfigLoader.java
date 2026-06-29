package net.sylphian.minecraft.cooking.config;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import net.sylphian.minecraft.cooking.recipe.IngredientSpec;
import net.sylphian.minecraft.cooking.recipe.MaterialIngredientSpec;
import net.sylphian.minecraft.cooking.recipe.NamespacedIngredientSpec;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses cooking recipe definitions from {@code recipes.yml}.
 *
 * <p>Each entry under the top-level {@code recipes} section becomes a
 * {@link CookingRecipe}. Ingredients are parsed as either a plain
 * Bukkit Material name (no colon) or a namespaced item ID (contains
 * a colon), matching the convention used by Sylphian-Items ItemRegistry.</p>
 */
public class RecipeConfigLoader {

    private final FileConfiguration config;
    private final Logger logger;

    /**
     * Constructs a new RecipeConfigLoader.
     *
     * @param config the loaded {@code recipes.yml}
     * @param logger the plugin logger for warnings
     */
    public RecipeConfigLoader(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Parses and returns all valid recipes from the configuration.
     *
     * @return list of parsed recipes; never null
     */
    public List<CookingRecipe> loadRecipes() {
        List<CookingRecipe> recipes = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("recipes");

        if (section == null) {
            logger.warning("No 'recipes' section found in recipes.yml, no recipes will be registered.");
            return recipes;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;

            // --- ingredients ---
            List<String> rawIngredients = entry.getStringList("ingredients");
            if (rawIngredients.isEmpty()) {
                logger.warning("Recipe '" + id + "' has no ingredients, skipping.");
                continue;
            }
            if (rawIngredients.size() > 5) {
                logger.warning("Recipe '" + id + "' has " + rawIngredients.size()
                        + " ingredients (max 5), skipping.");
                continue;
            }

            List<IngredientSpec> specs = new ArrayList<>();
            boolean valid = true;
            for (String raw : rawIngredients) {
                if (raw.contains(":")) {
                    specs.add(new NamespacedIngredientSpec(raw));
                } else {
                    try {
                        Material mat = Material.valueOf(raw.toUpperCase());
                        specs.add(new MaterialIngredientSpec(mat));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Recipe '" + id + "' has unknown material ingredient '"
                                + raw + "', skipping recipe.");
                        valid = false;
                        break;
                    }
                }
            }
            if (!valid) continue;

            // --- cook time ---
            int cookTime = entry.getInt("cook-time", 200);

            // --- output ---
            ConfigurationSection outputSection = entry.getConfigurationSection("output");
            if (outputSection == null) {
                logger.warning("Recipe '" + id + "' is missing an 'output' section, skipping.");
                continue;
            }

            String outputMaterialName = outputSection.getString("item");
            if (outputMaterialName == null) {
                logger.warning("Recipe '" + id + "' output is missing 'item', skipping.");
                continue;
            }

            Material outputMaterial;
            try {
                outputMaterial = Material.valueOf(outputMaterialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Recipe '" + id + "' output has unknown material '"
                        + outputMaterialName + "', skipping.");
                continue;
            }

            String displayName = outputSection.getString("display-name", "");
            String description = outputSection.getString("description", "");
            int amount = outputSection.getInt("amount", 1);

            float customModelData = outputSection.contains("custom-model-data")
                    ? (float) outputSection.getDouble("custom-model-data")
                    : -1f;

            List<String> lore = new ArrayList<>();
            if (!description.isEmpty()) {
                lore.addAll(List.of(description.split("\n")));
            }

            var builder = new ItemBuilder(outputMaterial).amount(amount);
            if (!displayName.isEmpty()) builder.name(displayName);
            if (!lore.isEmpty()) builder.loreStrings(lore);
            if (customModelData >= 0) builder.customModelData(customModelData);

            boolean qualityBonusEnabled = entry.getBoolean("quality-bonus", true);

            recipes.add(new CookingRecipe(id, List.copyOf(specs), cookTime, builder.build(), qualityBonusEnabled, customModelData));
        }

        logger.info("Recipes loaded [" + recipes.size() + "] entries registered.");
        return recipes;
    }
}
