package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.service.ClanInviteService.PendingInvite;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan accept <clan>} — accepts a pending invite and joins the clan. */
public final class AcceptSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public AcceptSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("accept")
                .executesPlayer((Player player, CommandArguments _) ->
                        player.sendMessage(MINI.deserialize("<red>Usage: /clan accept <clan_name>")))
                .then(new StringArgument("clan")
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                            if (info.sender() instanceof Player p) {
                                return ctx.inviteService().getPendingInvites(p.getUniqueId()).stream()
                                        .map(PendingInvite::clanName)
                                        .toArray(String[]::new);
                            }
                            return new String[0];
                        }))
                        .executesPlayer((Player player, CommandArguments args) -> {
                            if (ctx.clanCache().get(player.getUniqueId()).isPresent()) {
                                player.sendMessage(Component.text("You are already in a clan.", NamedTextColor.RED));
                                return;
                            }
                            String clanName = (String) args.get("clan");
                            ctx.inviteService().consumeInvite(player.getUniqueId(), clanName).ifPresentOrElse(
                                    invite -> ctx.clanService().addMember(invite.clanId(), player.getUniqueId(), ctx.inviteService())
                                            .thenRun(() -> player.sendMessage(MINI.deserialize("<green>You joined <white>" + clanName + "<green>!")))
                                            .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; }),
                                    () -> player.sendMessage(Component.text(
                                            "No active invite found from '" + clanName + "'.", NamedTextColor.RED))
                            );
                        }));
    }
}
