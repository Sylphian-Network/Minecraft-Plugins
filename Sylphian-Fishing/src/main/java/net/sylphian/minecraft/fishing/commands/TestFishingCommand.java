package net.sylphian.minecraft.fishing.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.config.MutationConfig;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.services.LootService;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Administrative command to run fishing catch simulations.
 * Allows testing of loot distribution and mutation rates under
 * different biomes, weather conditions, Y coordinates, and times of day.
 *
 * <p>Usage: /test_fishing &lt;count&gt; [biome] [weather] [y] [time]</p>
 *
 * <p>When run by a player, their current biome, weather, Y coordinate,
 * and world time are used as defaults for any omitted arguments.</p>
 */
public class TestFishingCommand implements BasicCommand {

    private final LootService lootService;
    private final ConfigLoader config;
    private final Random random = new Random();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Constructs a new TestFishingCommand.
     *
     * @param lootService the loot manager to use for simulations
     * @param config      the config loader for retrieving mutation chances
     */
    public TestFishingCommand(LootService lootService, ConfigLoader config) {
        this.lootService = lootService;
        this.config = config;
    }

    /**
     * Executes the simulation.
     * Runs the specified number of catch rolls and displays rarity
     * distribution and Super Fish mutation statistics to the sender.
     *
     * @param stack the command source stack
     * @param args  command arguments: &lt;count&gt; [biome] [weather] [y] [time]
     */
    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length < 1) {
            sender.sendMessage(Component.text(
                    "Usage: /test_fishing <count> [biome] [weather] [y]", NamedTextColor.RED));
            return;
        }

        int count;
        try {
            count = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid count: " + args[0], NamedTextColor.RED));
            return;
        }

        if (count <= 0) {
            sender.sendMessage(Component.text("Count must be greater than 0.", NamedTextColor.RED));
            return;
        }

        var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);

        Biome biome = null;
        if (args.length >= 2) {
            biome = biomeRegistry.get(NamespacedKey.minecraft(args[1].toLowerCase()));
            if (biome == null) {
                sender.sendMessage(Component.text("Invalid biome: " + args[1], NamedTextColor.RED));
                return;
            }
        }

        WeatherCondition weather = null;
        if (args.length >= 3) {
            try {
                weather = WeatherCondition.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text(
                        "Invalid weather: " + args[2]
                        + ". Valid values: " + Arrays.toString(WeatherCondition.values()),
                        NamedTextColor.RED));
                return;
            }
        }

        Double hookY = null;
        if (args.length >= 4) {
            try {
                hookY = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid Y coordinate: " + args[3], NamedTextColor.RED));
                return;
            }
        }

        Long worldTime = null;
        if (args.length >= 5) {
            try {
                worldTime = Long.parseLong(args[4]);
                if (worldTime < 0 || worldTime > 24000) {
                    sender.sendMessage(Component.text(
                            "Time must be between 0 and 24000 ticks.", NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text(
                        "Invalid time: " + args[4], NamedTextColor.RED));
                return;
            }
        }

        if (sender instanceof Player player) {
            if (biome == null) biome = player.getLocation().getBlock().getBiome();
            if (weather == null) weather = WeatherCondition.from(player.getWorld());
            if (hookY == null) hookY = player.getLocation().getY();
            if (worldTime == null) worldTime = player.getWorld().getTime();
        } else {
            if (biome == null) biome = biomeRegistry.get(NamespacedKey.minecraft("plains"));
            if (weather == null) weather = WeatherCondition.CLEAR;
            if (hookY == null) hookY = 62.0;
            if (worldTime == null) worldTime = 6000L;
        }

        if (biome == null) {
            sender.sendMessage(Component.text("Could not determine biome.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(miniMessage.deserialize(String.format(
                "<yellow>Running <white>%d <yellow>simulations — " +
                        "BIOME: <white>%s <yellow>WEATHER: <white>%s <yellow>Y: <white>%.0f <yellow>TIME: <white>%d",
                count, biome.getKey().value().toUpperCase(), weather.name(), hookY, worldTime)));

        List<Rarity> rarities = Rarity.byDescendingRarity();
        Map<Rarity, Integer> rarityCounts = new LinkedHashMap<>();
        Map<Rarity, Integer> superFishCounts = new LinkedHashMap<>();
        for (Rarity rarity : rarities) {
            rarityCounts.put(rarity, 0);
            superFishCounts.put(rarity, 0);
        }

        MutationConfig superFishConfig = config.getMutationConfig("super_fish");
        boolean superFishEnabled = superFishConfig.enabled();
        double superFishBaseChance = superFishConfig.baseChance();

        // Run the simulation loop
        for (int i = 0; i < count; i++) {
            CatchResult result = lootService.rollCatch(biome, weather, hookY, worldTime);
            Rarity rarity = result.rarity();
            if (rarity == null) continue;

            rarityCounts.merge(rarity, 1, Integer::sum);

            if (superFishEnabled) {
                double finalChance = superFishBaseChance * rarity.getMutationMultiplier();
                if (random.nextDouble() < finalChance) {
                    superFishCounts.merge(rarity, 1, Integer::sum);
                }
            }
        }

        sender.sendMessage(Component.text("--- Catch Results ---", NamedTextColor.GOLD));

        for (Rarity rarity : rarities) {
            int rCount = rarityCounts.getOrDefault(rarity, 0);
            int sCount = superFishCounts.getOrDefault(rarity, 0);
            int normalCount = rCount - sCount;

            double rPercent = (double) rCount / count * 100;
            double sPercent = rCount > 0 ? (double) sCount / rCount * 100 : 0;

            sender.sendMessage(miniMessage.deserialize(String.format(
                    "  %s%-12s <gray>%d (%.1f%%)",
                    rarity.getColor(), rarity.getId().toUpperCase(), rCount, rPercent)));

            if (superFishEnabled && rCount > 0) {
                sender.sendMessage(miniMessage.deserialize(String.format(
                        "  <gray>  ├ Normal:     %d", normalCount)));
                sender.sendMessage(miniMessage.deserialize(String.format(
                        "  <gray>  └ Super Fish: %d (%.1f%% of %s)",
                        sCount, sPercent, rarity.getId())));
            }
        }

        sender.sendMessage(Component.text("-----------------------------------", NamedTextColor.GRAY));

        int totalSuperFish = superFishCounts.values().stream().mapToInt(Integer::intValue).sum();
        sender.sendMessage(miniMessage.deserialize(String.format(
                "<gray>Total: <white>%d <gray>| Super Fish: <white>%d <gray>(%.1f%%)",
                count, totalSuperFish, (double) totalSuperFish / count * 100)));
    }

    /**
     * Provides tab completion suggestions for each argument position.
     *
     * @param stack the command source stack
     * @param args  the current command arguments
     * @return a collection of tab completion suggestions
     */
    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) { // Count
            return List.of("100", "250", "500", "1000", "2500", "5000");
        }

        if (args.length == 2) { // Biome
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream()
                    .map(b -> b.getKey().value().toLowerCase())
                    .toList();
        }

        if (args.length == 3) { // Weather
            return Arrays.stream(WeatherCondition.values())
                    .map(w -> w.name().toLowerCase())
                    .toList();
        }

        if (args.length == 4) { // Y coordinate
            return List.of("-59", "-30", "0", "30", "62", "100", "150", "200");
        }

        if (args.length == 5) { // Time
            return List.of(
                    "0",     // dawn
                    "1000",  // early morning
                    "6000",  // noon
                    "12000", // dusk
                    "13000", // early night
                    "18000", // midnight
                    "23000"  // late night
            );
        }

        return List.of();
    }

    /**
     * Restricts this command to players or admins with the appropriate permission.
     *
     * @param sender the command sender
     * @return true if the sender has permission
     */
    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("sylphian.fishing.admin");
    }
}
