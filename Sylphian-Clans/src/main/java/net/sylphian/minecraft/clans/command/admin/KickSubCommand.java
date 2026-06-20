package net.sylphian.minecraft.clans.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

import static net.sylphian.minecraft.clans.command.admin.ClanAdminContext.MINI;

/** {@code /sylphian-clans kick <player>} — force-removes a player from their clan. */
public final class KickSubCommand implements SubCommand {

    private final ClanAdminContext ctx;

    public KickSubCommand(ClanAdminContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("kick")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-clans kick <player>")))
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .executes((CommandSender sender, CommandArguments args) -> {
                            Player target = (Player) args.get("player");
                            ctx.clanService().getClanByPlayer(target.getUniqueId()).thenCompose(clanOpt -> {
                                if (clanOpt.isEmpty()) {
                                    sender.sendMessage(Component.text(target.getName() + " is not in a clan.", NamedTextColor.RED));
                                    return CompletableFuture.completedFuture(null);
                                }
                                return ctx.clanService().removeMember(clanOpt.get().clanId(), target.getUniqueId())
                                        .thenRun(() -> {
                                            sender.sendMessage(MINI.deserialize("<yellow>Force-kicked <white>" + target.getName() + "<yellow> from their clan."));
                                            target.sendMessage(MINI.deserialize("<red>You were removed from your clan by an administrator."));
                                        });
                            }).exceptionally(ex -> { sender.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                        }));
    }
}
