package net.sylphian.minecraft.fishing.loot;
import net.sylphian.minecraft.fishing.fish.FishEntry;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class LootManager {

    private final Map<Rarity, List<FishEntry>> poolsByRarity;
    private final Random random = new Random();

    public LootManager(List<FishEntry> allFish) {
        this.poolsByRarity = allFish.stream()
                .collect(Collectors.groupingBy(FishEntry::getRarity));
    }

    public CatchResult rollCatch(Biome biome) {
        double roll = random.nextDouble();

        for (Rarity rarity : Rarity.byDescendingRarity()) {
            if (roll > rarity.getChance()) continue;

            List<FishEntry> pool = poolsByRarity
                    .getOrDefault(rarity, List.of())
                    .stream()
                    .filter(f -> f.appliesToBiome(biome))
                    .toList();

            if (pool.isEmpty()) continue;

            return buildCatchResult(weightedPick(pool));
        }

        Rarity fallback = getFallbackRarity();

        if (fallback != null) {
            List<FishEntry> pool = poolsByRarity
                    .getOrDefault(fallback, List.of())
                    .stream()
                    .filter(f -> f.appliesToBiome(biome))
                    .toList();

            if (!pool.isEmpty()) {
                return buildCatchResult(weightedPick(pool));
            }

            return new CatchResult("fallback", fallback, 0.0, new ItemStack(Material.COD));
        }

        return new CatchResult("fallback", null, 0.0, new ItemStack(Material.COD));
    }

    private Rarity getFallbackRarity() {
        return Rarity.values().stream()
                .min(Comparator.comparingDouble(Rarity::getChance))
                .orElse(null);
    }

    private FishEntry weightedPick(List<FishEntry> pool) {
        int totalWeight = pool.stream().mapToInt(FishEntry::getWeight).sum();
        int roll = random.nextInt(totalWeight);
        int cursor = 0;

        for (FishEntry fish : pool) {
            cursor += fish.getWeight();
            if (roll < cursor) return fish;
        }

        return pool.getFirst();
    }

    private CatchResult buildCatchResult(FishEntry fish) {
        double weight = fish.rollWeight(random);
        ItemStack itemStack = buildItemStack(fish, weight);
        return new CatchResult(fish.getId(), fish.getRarity(), weight, itemStack);
    }

    private ItemStack buildItemStack(FishEntry fish, double caughtWeight) {
        return new ItemBuilder(fish.getMaterial())
                .name(fish.getDisplayName())
                .loreStrings(buildLore(fish, caughtWeight))
                .build();
    }

    private List<String> buildLore(FishEntry fish, double caughtWeight) {
        List<String> lore = new ArrayList<>();

        if (!fish.getDescription().isEmpty()) {
            lore.addAll(Arrays.asList(fish.getDescription().split("\n")));
            lore.add("");
        }

        lore.add(String.format("<gray>Rarity: %s", fish.getRarity().getDisplayName()));
        lore.add(String.format("<gray>Weight: <white>%.2fkg", caughtWeight));

        return lore;
    }
}