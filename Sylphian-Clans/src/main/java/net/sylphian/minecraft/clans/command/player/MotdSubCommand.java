package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan motd <message>} or {@code /clan motd clear} — sets or clears the clan MOTD. */
public final class MotdSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public MotdSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("motd")
                .executesPlayer((Player player, CommandArguments _) -> {
                    if (ctx.requirePermission(player, ClanPermission.SET_MOTD) != null) {
                        player.sendMessage(MINI.deserialize("<red>Usage: /clan motd <message>  ·  /clan motd clear"));
                    }
                })
                .then(new GreedyStringArgument("message")
                        .executesPlayer((Player player, CommandArguments args) -> {
                            Clan clan = ctx.requirePermission(player, ClanPermission.SET_MOTD);
                            if (clan == null) {
                                return;
                            }
                            String raw = (String) args.get("message");

                            if (raw.equalsIgnoreCase("clear")) {
                                ctx.clanService().setMotd(clan.clanId(), null)
                                        .thenRun(() -> player.sendMessage(MINI.deserialize("<yellow>Clan MOTD cleared.")))
                                        .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                                return;
                            }

                            if (raw.length() > 256) {
                                player.sendMessage(Component.text("MOTD is too long (max 256 characters).", NamedTextColor.RED));
                                return;
                            }

                            ctx.clanService().setMotd(clan.clanId(), raw)
                                    .thenRun(() -> player.sendMessage(MINI.deserialize("<yellow>Clan MOTD updated. Use <white>/clan info</white> to view it.")))
                                    .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                        }));
    }
}
