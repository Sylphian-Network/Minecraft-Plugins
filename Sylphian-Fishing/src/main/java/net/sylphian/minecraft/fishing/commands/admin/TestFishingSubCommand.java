package net.sylphian.minecraft.fishing.commands.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.commands.SubCommand;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;
import net.sylphian.minecraft.fishing.services.FishMutationService;
import net.sylphian.minecraft.fishing.services.LootService;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * {@code /sylphian-fishing test-fishing <count> [biome] [weather] [y] [time]} — simulates fishing
 * catches and prints rarity distribution and mutation statistics.
 *
 * <p>All arguments after {@code count} are optional. When omitted, the sender's current context is
 * used (biome, weather, Y, world time); console falls back to hardcoded defaults.</p>
 */
public final class TestFishingSubCommand implements SubCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final LootService lootService;
    private final FishMutationService mutationService;
    private final Random random = new Random();

    public TestFishingSubCommand(LootService lootService, FishMutationService mutationService) {
        this.lootService = lootService;
        this.mutationService = mutationService;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("test-fishing")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender))
                .then(new IntegerArgument("count", 1)
                        .executes((CommandSender sender, CommandArguments args) ->
                                simulate(sender, (int) args.get("count"), null, null, null, null))
                        .then(buildBiomeBranch()));
    }

    private Argument<?> buildBiomeBranch() {
        return new StringArgument("biome")
                .replaceSuggestions(ArgumentSuggestions.strings(info ->
                        RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
                                .stream().map(b -> b.getKey().value().toLowerCase()).toArray(String[]::new)))
                .executes((CommandSender sender, CommandArguments args) -> {
                    Biome biome = parseBiome(sender, (String) args.get("biome"));
                    if (biome == null) return;
                    simulate(sender, (int) args.get("count"), biome, null, null, null);
                })
                .then(buildWeatherBranch());
    }

    private Argument<?> buildWeatherBranch() {
        return new StringArgument("weather")
                .replaceSuggestions(ArgumentSuggestions.strings(info ->
                        Arrays.stream(WeatherCondition.values())
                                .map(w -> w.name().toLowerCase()).toArray(String[]::new)))
                .executes((CommandSender sender, CommandArguments args) -> {
                    Biome biome = parseBiome(sender, (String) args.get("biome"));
                    WeatherCondition weather = parseWeather(sender, (String) args.get("weather"));
                    if (biome == null || weather == null) return;
                    simulate(sender, (int) args.get("count"), biome, weather, null, null);
                })
                .then(buildYBranch());
    }

    private Argument<?> buildYBranch() {
        return new DoubleArgument("y")
                .executes((CommandSender sender, CommandArguments args) -> {
                    Biome biome = parseBiome(sender, (String) args.get("biome"));
                    WeatherCondition weather = parseWeather(sender, (String) args.get("weather"));
                    if (biome == null || weather == null) return;
                    simulate(sender, (int) args.get("count"), biome, weather, (double) args.get("y"), null);
                })
                .then(buildTimeBranch());
    }

    private Argument<?> buildTimeBranch() {
        return new IntegerArgument("time", 0, 24000)
                .executes((CommandSender sender, CommandArguments args) -> {
                    Biome biome = parseBiome(sender, (String) args.get("biome"));
                    WeatherCondition weather = parseWeather(sender, (String) args.get("weather"));
                    if (biome == null || weather == null) return;
                    simulate(sender, (int) args.get("count"), biome, weather, (double) args.get("y"), (long) (int) args.get("time"));
                });
    }

    private void simulate(CommandSender sender, int count, Biome biome, WeatherCondition weather,
                          Double hookY, Long worldTime) {
        var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);

        if (sender instanceof Player player) {
            if (biome == null)     biome     = player.getLocation().getBlock().getBiome();
            if (weather == null)   weather   = WeatherCondition.from(player.getWorld());
            if (hookY == null)     hookY     = player.getLocation().getY();
            if (worldTime == null) worldTime = player.getWorld().getTime();
        } else {
            if (biome == null)     biome     = biomeRegistry.get(NamespacedKey.minecraft("plains"));
            if (weather == null)   weather   = WeatherCondition.CLEAR;
            if (hookY == null)     hookY     = 62.0;
            if (worldTime == null) worldTime = 6000L;
        }

        if (biome == null) {
            sender.sendMessage(Component.text("Could not determine biome.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(MINI.deserialize(String.format(
                "<yellow>Running <white>%d <yellow>simulations — " +
                        "BIOME: <white>%s <yellow>WEATHER: <white>%s <yellow>Y: <white>%.0f <yellow>TIME: <white>%d",
                count, biome.getKey().value().toUpperCase(), weather.name(), hookY, worldTime)));

        List<Rarity> rarities = Rarity.byDescendingRarity();
        Map<Rarity, Integer> rarityCounts = new LinkedHashMap<>();
        Map<Rarity, Integer> superFishCounts = new LinkedHashMap<>();
        for (Rarity r : rarities) {
            rarityCounts.put(r, 0);
            superFishCounts.put(r, 0);
        }

        var superFishConfig = mutationService.getMutationConfig("super_fish");
        boolean superFishEnabled = superFishConfig.enabled();
        double superFishBaseChance = superFishConfig.baseChance();

        for (int i = 0; i < count; i++) {
            CatchResult result;
            try {
                result = lootService.rollCatch(biome, weather, hookY, worldTime);
            } catch (IllegalStateException e) {
                sender.sendMessage(Component.text("No loot configured for this context: " + e.getMessage(), NamedTextColor.RED));
                return;
            }
            Rarity r = result.rarity();
            if (r == null) continue;
            rarityCounts.merge(r, 1, Integer::sum);
            if (superFishEnabled) {
                double finalChance = superFishBaseChance * r.getMutationMultiplier();
                if (random.nextDouble() < finalChance) superFishCounts.merge(r, 1, Integer::sum);
            }
        }

        sender.sendMessage(Component.text("--- Catch Results ---", NamedTextColor.GOLD));
        for (Rarity r : rarities) {
            int rCount = rarityCounts.getOrDefault(r, 0);
            int sCount = superFishCounts.getOrDefault(r, 0);
            double rPercent = (double) rCount / count * 100;
            double sPercent = rCount > 0 ? (double) sCount / rCount * 100 : 0;
            sender.sendMessage(MINI.deserialize(String.format(
                    "  %s%-12s <gray>%d (%.1f%%)", r.getColor(), r.getId().toUpperCase(), rCount, rPercent)));
            if (superFishEnabled && rCount > 0) {
                sender.sendMessage(MINI.deserialize(String.format("  <gray>  ├ Normal:     %d", rCount - sCount)));
                sender.sendMessage(MINI.deserialize(String.format(
                        "  <gray>  └ Super Fish: %d (%.1f%% of %s)", sCount, sPercent, r.getId())));
            }
        }

        sender.sendMessage(Component.text("-----------------------------------", NamedTextColor.GRAY));
        int totalSuperFish = superFishCounts.values().stream().mapToInt(Integer::intValue).sum();
        sender.sendMessage(MINI.deserialize(String.format(
                "<gray>Total: <white>%d <gray>| Super Fish: <white>%d <gray>(%.1f%%)",
                count, totalSuperFish, (double) totalSuperFish / count * 100)));
    }

    private Biome parseBiome(CommandSender sender, String input) {
        Biome biome = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
                .get(NamespacedKey.minecraft(input.toLowerCase()));
        if (biome == null) {
            sender.sendMessage(Component.text("Invalid biome: " + input, NamedTextColor.RED));
        }
        return biome;
    }

    private WeatherCondition parseWeather(CommandSender sender, String input) {
        try {
            return WeatherCondition.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid weather: " + input
                    + ". Valid values: " + Arrays.toString(WeatherCondition.values()), NamedTextColor.RED));
            return null;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Usage: /sylphian-fishing test-fishing <count> [biome] [weather] [y] [time]", NamedTextColor.RED));
    }
}
