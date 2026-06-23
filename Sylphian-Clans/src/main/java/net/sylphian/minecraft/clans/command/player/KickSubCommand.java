package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.event.ClanMemberLeaveEvent;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan kick <player>} — removes a member from the sender's clan. */
public final class KickSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public KickSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("kick")
                .executesPlayer((Player player, CommandArguments _) ->
                        player.sendMessage(MINI.deserialize("<red>Usage: /clan kick <player>")))
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .executesPlayer((Player player, CommandArguments args) -> {
                            Clan clan = ctx.requirePermission(player, ClanPermission.KICK_MEMBERS);
                            if (clan == null) {
                                return;
                            }
                            Player target = (Player) args.get("player");

                            if (!clan.isMember(target.getUniqueId())) {
                                player.sendMessage(Component.text(target.getName() + " is not in your clan.", NamedTextColor.RED));
                                return;
                            }
                            if (clan.leaderId().map(target.getUniqueId()::equals).orElse(false)) {
                                player.sendMessage(Component.text("You cannot kick the clan leader.", NamedTextColor.RED));
                                return;
                            }

                            ctx.clanService().removeMember(clan.clanId(), target.getUniqueId(), ClanMemberLeaveEvent.Cause.KICK)
                                    .thenRun(() -> {
                                        player.sendMessage(MINI.deserialize("<yellow>Kicked <white>" + target.getName() + "<yellow> from the clan."));
                                        target.sendMessage(MINI.deserialize("<red>You were kicked from <white>" + clan.name() + "<red>."));
                                    })
                                    .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                        }));
    }
}
