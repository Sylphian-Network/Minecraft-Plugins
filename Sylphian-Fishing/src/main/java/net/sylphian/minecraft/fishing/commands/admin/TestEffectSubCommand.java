package net.sylphian.minecraft.fishing.commands.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.commands.SubCommand;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.services.CatchEffectService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * {@code /sylphian-fishing test-effect <rarity>} — triggers rarity catch effects at the player's
 * target location.
 */
public final class TestEffectSubCommand implements SubCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final double RAY_DISTANCE = 5.0;

    private final CatchEffectService catchEffectService;

    public TestEffectSubCommand(CatchEffectService catchEffectService) {
        this.catchEffectService = catchEffectService;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("test-effect")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-fishing test-effect <rarity>")))
                .then(new StringArgument("rarity")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                Rarity.values().stream().map(r -> r.getId().toLowerCase()).toArray(String[]::new)))
                        .executesPlayer((Player player, CommandArguments args) -> {
                            String rarityId = ((String) args.get("rarity")).toUpperCase();
                            Rarity rarity = Rarity.getById(rarityId);
                            if (rarity == null) {
                                player.sendMessage(MINI.deserialize("<red>Unknown rarity '" + args.get("rarity")
                                        + "'. Valid rarities: " + Rarity.values().stream().map(Rarity::getId).toList()));
                                return;
                            }
                            Location effectLocation = resolveEffectLocation(player);
                            catchEffectService.apply(player, rarity, rarity.getId().toLowerCase(), effectLocation);
                            player.sendMessage(MINI.deserialize("Triggered effects for rarity: " + rarity.getDisplayName()));
                        }));
    }

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
}
