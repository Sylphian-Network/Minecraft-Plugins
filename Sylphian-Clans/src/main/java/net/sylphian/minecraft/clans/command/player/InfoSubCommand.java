package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanRole;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.DATE_FMT;
import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan info [clan]} — shows the sender's clan, or a named clan. */
public final class InfoSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public InfoSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("info")
                .executesPlayer((Player player, CommandArguments _) ->
                        ctx.clanCache().get(player.getUniqueId()).ifPresentOrElse(
                                clan -> printClanInfo(player, clan),
                                () -> player.sendMessage(Component.text("You are not in a clan. Use /clan info <name> to look up another clan.", NamedTextColor.GRAY))
                        ))
                .then(new StringArgument("clan")
                        .executesPlayer((Player player, CommandArguments args) -> {
                            String name = (String) args.get("clan");
                            ctx.clanService().getClanByName(name).thenAccept(opt ->
                                    opt.ifPresentOrElse(
                                            clan -> printClanInfo(player, clan),
                                            () -> player.sendMessage(Component.text("No clan named '" + name + "' found.", NamedTextColor.RED))
                                    )
                            ).exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                        }));
    }

    private void printClanInfo(Player player, Clan clan) {
        player.sendMessage(MINI.deserialize("<yellow>--- <white>" + clan.name() + " <yellow>---"));
        if (clan.motd() != null && !clan.motd().isBlank()) {
            player.sendMessage(MINI.deserialize("<gray>MOTD: <reset>" + clan.motd()));
        }
        player.sendMessage(MINI.deserialize("<gray>Founded: <white>" + DATE_FMT.format(clan.createdAt())));
        player.sendMessage(MINI.deserialize("<gray>Members: <white>" + clan.members().size()));
        clan.members().forEach(m -> {
            String roleBadge = m.role() == ClanRole.LEADER ? "<gold>[L]</gold> " : "<gray>[M]</gray> ";
            player.sendMessage(MINI.deserialize("  " + roleBadge + "<white>" + ctx.resolvePlayerName(m.playerId())));
        });
    }
}
