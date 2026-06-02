package net.sylphian.minecraft.fishing.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.SylphianFishing;
import net.sylphian.minecraft.fishing.config.BaitConfig;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;
import net.sylphian.minecraft.fishing.services.BaitZoneService;
import net.sylphian.minecraft.fishing.services.CatchEffectService;
import net.sylphian.minecraft.fishing.services.FishMutationService;
import net.sylphian.minecraft.fishing.services.LootService;
import net.sylphian.minecraft.fishing.services.bait.BaitItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Root administrative command for Sylphian Fishing.
 *
 * <p>Usage: {@code /sylphian-fishing <subcommand>}</p>
 *
 * <ul>
 *   <li>{@code reload} — reloads config.yml and fish.yml without restarting</li>
 *   <li>{@code test-effect <rarity>} — triggers rarity catch effects at the player's target location</li>
 *   <li>{@code test-fishing <count> [biome] [weather] [y] [time]} — simulates fishing catches and prints distribution</li>
 *   <li>{@code give-bait <bait-id> [amount]} — gives the executing player a bait item</li>
 * </ul>
 */
public class SylphianFishingCommand implements BasicCommand {

    private static final double RAY_DISTANCE = 5.0;
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SylphianFishing plugin;
    private final CatchEffectService catchEffectService;
    private final LootService lootService;
    private final FishMutationService mutationService;
    private final BaitZoneService baitZoneService;
    private final Random random = new Random();

    /**
     * Constructs a new SylphianFishingCommand.
     *
     * @param plugin             the plugin instance used to trigger reloads
     * @param catchEffectService the service used to apply rarity catch effects
     * @param lootService        the loot service used for fishing simulations
     * @param mutationService    the mutation service used for simulation statistics
     * @param baitZoneService the service used to look up bait configs and provide tab completion
     */
    public SylphianFishingCommand(SylphianFishing plugin, CatchEffectService catchEffectService,
                                  LootService lootService, FishMutationService mutationService,
                                  BaitZoneService baitZoneService) {
        this.plugin = plugin;
        this.catchEffectService = catchEffectService;
        this.lootService = lootService;
        this.mutationService = mutationService;
        this.baitZoneService = baitZoneService;
    }

    /**
     * Routes execution to the appropriate subcommand handler.
     *
     * @param stack the command source stack
     * @param args  the command arguments
     */
    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload"       -> handleReload(sender);
            case "test-effect"  -> handleTestEffect(sender, args);
            case "test-fishing" -> handleTestFishing(sender, args);
            case "give-bait"    -> handleGiveBait(sender, args);
            default             -> sendUsage(sender);
        }
    }

    /**
     * Reloads all plugin configuration from disk.
     *
     * @param sender the command sender to notify
     */
    private void handleReload(CommandSender sender) {
        sender.sendMessage(Component.text("Reloading Sylphian Fishing configuration...", NamedTextColor.YELLOW));
        plugin.reload(sender);
    }

    /**
     * Triggers rarity catch effects at the player's target location.
     *
     * @param sender the command sender (must be a player)
     * @param args   full args array; args[1] is the rarity
     */
    private void handleTestEffect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This subcommand can only be used by a player.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /sylphian-fishing test-effect <rarity>", NamedTextColor.RED));
            return;
        }

        Rarity rarity = Rarity.getById(args[1].toUpperCase());
        if (rarity == null) {
            sender.sendMessage(Component.text("Unknown rarity '" + args[1] + "'. Valid rarities: "
                    + Rarity.values().stream().map(Rarity::getId).toList(), NamedTextColor.RED));
            return;
        }

        Location effectLocation = resolveEffectLocation(player);
        CatchResult result = new CatchResult("test", rarity, 1.0, new ItemStack(Material.COD));

        catchEffectService.apply(player, result, effectLocation);
        player.sendMessage(MINI.deserialize("Triggered effects for rarity: " + rarity.getDisplayName()));
    }

    /**
     * Resolves the location where effects should be spawned by raycasting
     * to the block the player is looking at and finding the nearest clear space.
     *
     * @param player the player to raycast from
     * @return the resolved effect spawn location
     */
    private Location resolveEffectLocation(Player player) {
        Block targetBlock = player.getTargetBlockExact((int) RAY_DISTANCE);

        if (targetBlock == null) {
            return player.getEyeLocation().add(player.getLocation().getDirection().multiply(RAY_DISTANCE));
        }

        Block above = targetBlock.getRelative(0, 1, 0);
        if (above.getType().isAir()) {
            return above.getLocation().add(0.5, 0.5, 0.5);
        }

        Location eye = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection().normalize();

        for (double dist = RAY_DISTANCE; dist > 0; dist -= 0.5) {
            Location candidate = eye.clone().add(direction.clone().multiply(dist));
            if (candidate.getBlock().getType().isAir()) return candidate;
        }

        return player.getLocation();
    }

    /**
     * Simulates fishing catches and prints rarity distribution and mutation statistics.
     *
     * @param sender the command sender
     * @param args   full args array; args[1..5] are count, biome, weather, y, time
     */
    private void handleTestFishing(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text(
                    "Usage: /sylphian-fishing test-fishing <count> [biome] [weather] [y] [time]", NamedTextColor.RED));
            return;
        }

        int count;
        try {
            count = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid count: " + args[1], NamedTextColor.RED));
            return;
        }

        if (count <= 0) {
            sender.sendMessage(Component.text("Count must be greater than 0.", NamedTextColor.RED));
            return;
        }

        var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);

        Biome biome = null;
        if (args.length >= 3) {
            biome = biomeRegistry.get(NamespacedKey.minecraft(args[2].toLowerCase()));
            if (biome == null) {
                sender.sendMessage(Component.text("Invalid biome: " + args[2], NamedTextColor.RED));
                return;
            }
        }

        WeatherCondition weather = null;
        if (args.length >= 4) {
            try {
                weather = WeatherCondition.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text("Invalid weather: " + args[3]
                        + ". Valid values: " + Arrays.toString(WeatherCondition.values()), NamedTextColor.RED));
                return;
            }
        }

        Double hookY = null;
        if (args.length >= 5) {
            try {
                hookY = Double.parseDouble(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid Y coordinate: " + args[4], NamedTextColor.RED));
                return;
            }
        }

        Long worldTime = null;
        if (args.length >= 6) {
            try {
                worldTime = Long.parseLong(args[5]);
                if (worldTime < 0 || worldTime > 24000) {
                    sender.sendMessage(Component.text("Time must be between 0 and 24000 ticks.", NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid time: " + args[5], NamedTextColor.RED));
                return;
            }
        }

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
            CatchResult result = lootService.rollCatch(biome, weather, hookY, worldTime);
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

    /**
     * Gives the executing player a bait item of the specified type and amount.
     *
     * @param sender the command sender (must be a player)
     * @param args   full args array; args[1] is the bait ID, args[2] is the optional amount
     */
    private void handleGiveBait(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This subcommand can only be used by a player.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /sylphian-fishing give-bait <bait-id> [amount]", NamedTextColor.RED));
            return;
        }

        BaitConfig config = baitZoneService.getBaitConfig(args[1]);
        if (config == null) {
            sender.sendMessage(Component.text("Unknown bait '" + args[1] + "'. Valid baits: "
                    + baitZoneService.getBaitIds(), NamedTextColor.RED));
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage(Component.text("Amount must be between 1 and 64.", NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid amount: " + args[2], NamedTextColor.RED));
                return;
            }
        }

        ItemStack baitItem = BaitItem.create(config, plugin);
        baitItem.setAmount(amount);
        player.getInventory().addItem(baitItem);
        player.sendMessage(MINI.deserialize(
                "<gray>Given <white>" + amount + "x " + config.displayName() + "<gray>."));
    }

    /**
     * Sends usage information listing all available subcommands.
     *
     * @param sender the command sender to notify
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        sender.sendMessage(Component.text("  /sylphian-fishing reload", NamedTextColor.RED));
        sender.sendMessage(Component.text("  /sylphian-fishing test-effect <rarity>", NamedTextColor.RED));
        sender.sendMessage(Component.text("  /sylphian-fishing test-fishing <count> [biome] [weather] [y] [time]", NamedTextColor.RED));
        sender.sendMessage(Component.text("  /sylphian-fishing give-bait <bait-id> [amount]", NamedTextColor.RED));
    }

    /**
     * Provides tab completion for all subcommands and their arguments.
     *
     * @param stack the command source stack
     * @param args  the current arguments
     * @return available suggestions for the current argument position
     */
    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            return List.of("reload", "test-effect", "test-fishing", "give-bait");
        }

        return switch (args[0].toLowerCase()) {
            case "test-effect" -> args.length == 2
                    ? Rarity.values().stream().map(r -> r.getId().toLowerCase()).toList()
                    : List.of();

            case "test-fishing" -> switch (args.length) {
                case 2 -> List.of("100", "250", "500", "1000", "2500", "5000");
                case 3 -> RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
                        .stream().map(b -> b.getKey().value().toLowerCase()).toList();
                case 4 -> Arrays.stream(WeatherCondition.values())
                        .map(w -> w.name().toLowerCase()).toList();
                case 5 -> List.of("-59", "-30", "0", "30", "62", "100", "150", "200");
                case 6 -> List.of("0", "1000", "6000", "12000", "13000", "18000", "23000");
                default -> List.of();
            };

            case "give-bait" -> switch (args.length) {
                case 2 -> baitZoneService.getBaitIds().stream().toList();
                case 3 -> List.of("1", "4", "8", "16", "32", "64");
                default -> List.of();
            };

            default -> List.of();
        };
    }

    /**
     * Restricts this command to senders with the admin permission.
     *
     * @param sender the command sender
     * @return true if the sender has {@code sylphian.fishing.admin}
     */
    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("sylphian.fishing.admin");
    }
}