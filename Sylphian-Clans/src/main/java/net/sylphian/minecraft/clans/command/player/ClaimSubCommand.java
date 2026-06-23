package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan claim [radius]} — claims the current chunk, optionally a square out to a radius (0-5). */
public final class ClaimSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public ClaimSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("claim")
                .executesPlayer((Player player, CommandArguments _) -> doClaim(player, 0))
                .then(new IntegerArgument("radius", 0, 5)
                        .executesPlayer((Player player, CommandArguments args) ->
                                doClaim(player, (int) args.get("radius"))));
    }

    private void doClaim(Player player, int radius) {
        Clan clan = ctx.requirePermission(player, ClanPermission.CLAIM_TERRITORY);
        if (clan == null) {
            return;
        }

        Chunk centre = player.getLocation().getChunk();
        String world = centre.getWorld().getName();

        List<int[]> chunks = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                chunks.add(new int[]{centre.getX() + dx, centre.getZ() + dz});
            }
        }

        int total = chunks.size();
        ctx.territoryService().claimChunks(clan.clanId(), world, chunks)
                .thenAccept(claimedChunks -> {
                    int claimed = claimedChunks.size();
                    if (claimed == 0) {
                        player.sendMessage(Component.text(
                                "No chunks were claimed. They may already be taken or you have hit the limit.",
                                NamedTextColor.RED));
                    } else if (claimed == total) {
                        player.sendMessage(MINI.deserialize(
                                "<green>Claimed <white>" + claimed + " <green>chunk"
                                        + (claimed == 1 ? "" : "s") + " for <white>" + clan.name() + "<green>."));
                    } else {
                        player.sendMessage(MINI.deserialize(
                                "<yellow>Claimed <white>" + claimed + "<yellow>/<white>" + total
                                        + "<yellow> chunks. The rest were already taken or at the limit."));
                    }
                })
                .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }
}
