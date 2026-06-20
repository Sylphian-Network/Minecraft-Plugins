package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan disband} — leader-only; permanently deletes the clan. */
public final class DisbandSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public DisbandSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("disband")
                .executesPlayer((Player player, CommandArguments _) -> {
                    Clan clan = ctx.requireLeader(player);
                    if (clan == null) {
                        return;
                    }
                    ctx.clanService().disbandClan(clan.clanId())
                            .thenRun(() -> player.sendMessage(MINI.deserialize("<red>Clan <white>" + clan.name() + " <red>has been disbanded.")))
                            .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                });
    }
}
