package net.sylphian.minecraft.fishing.commands;
    
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.loot.LootManager;
import net.sylphian.minecraft.fishing.weather.WeatherCondition;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Administrative command to run fishing catch simulations.
 * Allows testing of loot distribution and mutation rates under different biomes and weather conditions.
 * Usage: /test_fishing [count] [biome] [weather]
 */
public class TestFishingCommand implements BasicCommand {

    private final LootManager lootManager;
    private final ConfigLoader config;
    private final Random random = new Random();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Constructs a new TestFishingCommand.
     *
     * @param lootManager the loot manager to use for simulations
     * @param config      the config loader for retrieving mutation chances
     */
    public TestFishingCommand(LootManager lootManager, ConfigLoader config) {
        this.lootManager = lootManager;
        this.config = config;
    }

    /**
     * Executes the simulation.
     * Runs the specified number of catch rolls and displays statistics to the sender.
     *
     * @param stack the command source stack
     * @param args  command arguments: [count] [biome] [weather]
     */
    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length < 1) {
            stack.getSender().sendMessage(Component.text("Usage: /test_fishing <count> [biome] [weather]", NamedTextColor.RED));
            return;
        }

        int count;
        try {
            count = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            stack.getSender().sendMessage(Component.text("Invalid count: " + args[0], NamedTextColor.RED));
            return;
        }

        if (count <= 0) {
            stack.getSender().sendMessage(Component.text("Count must be greater than 0.", NamedTextColor.RED));
            return;
        }

        Biome biome = null;
        var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);

        if (args.length >= 2) {
            biome = biomeRegistry.get(NamespacedKey.minecraft(args[1].toLowerCase()));
            if (biome == null) {
                stack.getSender().sendMessage(Component.text("Invalid biome: " + args[1], NamedTextColor.RED));
                return;
            }
        }

        WeatherCondition weather = null;
        if (args.length >= 3) {
            try {
                weather = WeatherCondition.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                stack.getSender().sendMessage(Component.text("Invalid weather: " + args[2], NamedTextColor.RED));
                return;
            }
        }

        // Defaults if not provided and sender is a player
        if (stack.getSender() instanceof Player player) {
            if (biome == null) biome = player.getLocation().getBlock().getBiome();
            if (weather == null) weather = WeatherCondition.from(player.getWorld());
        } else {
            if (biome == null) biome = biomeRegistry.get(NamespacedKey.minecraft("plains"));
            if (weather == null) weather = WeatherCondition.CLEAR;
        }

        if (biome == null) {
            stack.getSender().sendMessage(Component.text("Could not determine biome.", NamedTextColor.RED));
            return;
        }

        stack.getSender().sendMessage(miniMessage.deserialize(
                String.format("<yellow>Running %d catch simulations on BIOME: <white>%s <yellow>WEATHER: <white>%s <yellow>...",
                        count, biome.getKey().value().toUpperCase(), weather.name())));

        Map<Rarity, Integer> rarityCounts = new LinkedHashMap<>();
        Map<Rarity, Integer> superFishCounts = new LinkedHashMap<>();

        List<Rarity> rarities = Rarity.byDescendingRarity();
        for (Rarity rarity : rarities) {
            rarityCounts.put(rarity, 0);
            superFishCounts.put(rarity, 0);
        }

        double superFishBaseChance = config.getMutationBaseChance("super_fish");
        boolean superFishEnabled = config.isMutationEnabled("super_fish");

        // Run the simulation loop
        for (int i = 0; i < count; i++) {
            CatchResult result = lootManager.rollCatch(biome, weather);
            Rarity rarity = result.rarity();
            if (rarity == null) continue;

            rarityCounts.put(rarity, rarityCounts.getOrDefault(rarity, 0) + 1);

            // Simulate mutation roll based on configuration
            if (superFishEnabled) {
                double finalSuperFishChance = superFishBaseChance * rarity.getMutationMultiplier();
                if (random.nextDouble() < finalSuperFishChance) {
                    superFishCounts.put(rarity, superFishCounts.getOrDefault(rarity, 0) + 1);
                }
            }
        }

        stack.getSender().sendMessage(Component.text("-----------------------------------", NamedTextColor.GRAY));

        for (Rarity rarity : rarities) {
            int rCount = rarityCounts.getOrDefault(rarity, 0);
            double rPercent = (double) rCount / count * 100;
            stack.getSender().sendMessage(miniMessage.deserialize(
                    String.format("  %s%-12s <gray>%d (%.1f%%)", rarity.getColor(), rarity.getId().toUpperCase(), rCount, rPercent)));
        }

        stack.getSender().sendMessage(Component.text("-----------------------------------", NamedTextColor.GRAY));

        for (Rarity rarity : rarities) {
            int sCount = superFishCounts.getOrDefault(rarity, 0);
            double sPercent = (double) sCount / count * 100;
            stack.getSender().sendMessage(miniMessage.deserialize(
                    String.format("  %s%-12s <gray>[Super Fish]      <gray>%d (%.1f%%)", rarity.getColor(), rarity.getId().toUpperCase(), sCount, sPercent)));
        }

        stack.getSender().sendMessage(Component.text("-----------------------------------", NamedTextColor.GRAY));
    }

    /**
     * Provides tab completion suggestions for the command arguments.
     *
     * @param stack the command source stack
     * @param args  current command arguments
     * @return a collection of suggestions
     */
    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) return List.of("100", "250", "500", "750", "1000", "2500", "5000");

        if (args.length == 2) return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream()
                    .map(b -> b.getKey().value().toLowerCase())
                    .toList();

        if (args.length == 3) return Arrays.stream(WeatherCondition.values()).map(w -> w.name().toLowerCase()).toList();

        return List.of();
    }
}
