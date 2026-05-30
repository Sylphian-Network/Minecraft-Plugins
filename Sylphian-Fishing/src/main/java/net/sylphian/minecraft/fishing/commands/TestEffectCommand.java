package net.sylphian.minecraft.fishing.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.effects.CatchEffectService;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.fish.Rarity;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Command to test rarity catch effects at the player's target location.
 * Resolves the effect spawn point by raycasting to the block the player
 * is looking at and finding the nearest clear space.
 *
 * <p>Usage: /testeffect [rarity]</p>
 */
public class TestEffectCommand implements BasicCommand {

    private static final double RAY_DISTANCE = 5.0;

    private final CatchEffectService catchEffectService;

    /**
     * Constructs a new TestEffectCommand.
     *
     * @param catchEffectService the service used to apply rarity catch effects
     */
    public TestEffectCommand(CatchEffectService catchEffectService) {
        this.catchEffectService = catchEffectService;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("Usage: /testeffect <rarity>");
            return;
        }

        Rarity rarity = Rarity.getById(args[0].toUpperCase());
        if (rarity == null) {
            sender.sendMessage("Unknown rarity '" + args[0] + "'. Valid rarities: "
                    + Rarity.values().stream()
                    .map(Rarity::getId)
                    .toList());
            return;
        }

        Location effectLocation = resolveEffectLocation(player);

        // Build a minimal CatchResult so CatchEffectService has the data it needs
        ItemStack dummyItem = new ItemStack(Material.COD);
        CatchResult result = new CatchResult("test", rarity, 1.0, dummyItem);

        catchEffectService.apply(player, result, effectLocation);

        player.sendMessage(MiniMessage.miniMessage().deserialize("Triggered effects for rarity: " + rarity.getDisplayName()));
    }

    /**
     * Resolves the location where effects should be spawned.
     * Walks the ray from the player's eye toward the target block and returns
     * the last clear location before the block, offset upward by one block.
     * Falls back to the block above the target if that space is clear,
     * otherwise returns the player's own location.
     *
     * @param player the player to raycast from
     * @return the resolved effect spawn location
     */
    private Location resolveEffectLocation(Player player) {
        Block targetBlock = player.getTargetBlockExact((int) RAY_DISTANCE);

        if (targetBlock == null) {
            return player.getEyeLocation()
                    .add(player.getLocation().getDirection().multiply(RAY_DISTANCE));
        }

        Block above = targetBlock.getRelative(0, 1, 0);
        if (above.getType().isAir()) {
            return above.getLocation().add(0.5, 0.5, 0.5);
        }

        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector direction = player.getLocation()
                .getDirection().normalize();

        for (double dist = RAY_DISTANCE; dist > 0; dist -= 0.5) {
            Location candidate = eye.clone().add(direction.clone().multiply(dist));
            if (candidate.getBlock().getType().isAir()) {
                return candidate;
            }
        }

        return player.getLocation();
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) {
            return Rarity.values().stream()
                    .map(r -> r.getId().toLowerCase())
                    .toList();
        }
        return List.of();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission("sylphian.fishing.admin");
    }
}