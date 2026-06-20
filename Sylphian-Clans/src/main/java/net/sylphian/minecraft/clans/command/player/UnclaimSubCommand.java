package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan unclaim [all]} — releases the current chunk, or all territory (leader only). */
public final class UnclaimSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public UnclaimSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("unclaim")
                .executesPlayer((Player player, CommandArguments _) -> unclaimHere(player))
                .then(new LiteralArgument("all")
                        .executesPlayer((Player player, CommandArguments _) -> unclaimAll(player)));
    }

    private void unclaimAll(Player player) {
        Clan clan = ctx.requirePermission(player, ClanPermission.UNCLAIM_TERRITORY);
        if (clan == null) {
            return;
        }
        if (!clan.leaderId().map(player.getUniqueId()::equals).orElse(false)) {
            player.sendMessage(Component.text("Only the clan leader can unclaim all territory.", NamedTextColor.RED));
            return;
        }
        ctx.territoryService().unclaimAll(clan.clanId())
                .thenRun(() -> player.sendMessage(MINI.deserialize("<yellow>All territory unclaimed.")))
                .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void unclaimHere(Player player) {
        Clan clan = ctx.requirePermission(player, ClanPermission.UNCLAIM_TERRITORY);
        if (clan == null) {
            return;
        }
        Chunk chunk = player.getLocation().getChunk();
        ctx.territoryService().unclaimChunk(clan.clanId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ())
                .thenRun(() -> player.sendMessage(MINI.deserialize(
                        "<yellow>Chunk [<white>" + chunk.getX() + "<yellow>, <white>" + chunk.getZ() + "<yellow>] unclaimed.")))
                .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }
}
