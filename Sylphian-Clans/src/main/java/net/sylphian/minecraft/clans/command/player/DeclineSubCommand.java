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

/** {@code /clan decline <clan>} — discards a pending invite. */
public final class DeclineSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public DeclineSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("decline")
                .executesPlayer((Player player, CommandArguments _) ->
                        player.sendMessage(MINI.deserialize("<red>Usage: /clan decline <clan_name>")))
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
                            String clanName = (String) args.get("clan");
                            ctx.inviteService().consumeInvite(player.getUniqueId(), clanName).ifPresentOrElse(
                                    _ -> player.sendMessage(MINI.deserialize("<yellow>Declined invite from <white>" + clanName + "<yellow>.")),
                                    () -> player.sendMessage(Component.text("No active invite found from '" + clanName + "'.", NamedTextColor.RED))
                            );
                        }));
    }
}
