package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan delhome} — removes the clan home. */
public final class DelHomeSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public DelHomeSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("delhome")
                .executesPlayer((Player player, CommandArguments _) -> {
                    Clan clan = ctx.requirePermission(player, ClanPermission.SET_HOME);
                    if (clan == null) {
                        return;
                    }
                    ctx.clanService().deleteHome(clan.clanId())
                            .thenRun(() -> player.sendMessage(
                                    MINI.deserialize("<yellow>Clan home removed.")))
                            .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                });
    }
}
