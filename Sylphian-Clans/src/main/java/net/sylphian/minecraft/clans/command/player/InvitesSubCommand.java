package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.service.ClanInviteService.PendingInvite;
import org.bukkit.entity.Player;

import java.util.List;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan invites} — lists the sender's pending invites. */
public final class InvitesSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public InvitesSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("invites")
                .executesPlayer((Player player, CommandArguments _) -> {
                    List<PendingInvite> invites = ctx.inviteService().getPendingInvites(player.getUniqueId());
                    if (invites.isEmpty()) {
                        player.sendMessage(Component.text("You have no pending clan invites.", NamedTextColor.GRAY));
                        return;
                    }
                    player.sendMessage(MINI.deserialize("<yellow>Pending invites:"));
                    invites.forEach(i -> player.sendMessage(MINI.deserialize(
                            "<gray>  - <white>" + i.clanName() + " <gray>(invited by <white>" +
                            ctx.resolvePlayerName(i.inviterUuid()) + "<gray>)")));
                });
    }
}
