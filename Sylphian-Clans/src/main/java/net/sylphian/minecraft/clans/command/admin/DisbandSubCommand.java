package net.sylphian.minecraft.clans.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

import static net.sylphian.minecraft.clans.command.admin.ClanAdminContext.MINI;

/** {@code /sylphian-clans disband <clan_name>} — force-disbands a clan, bypassing permission checks. */
public final class DisbandSubCommand implements SubCommand {

    private final ClanAdminContext ctx;

    public DisbandSubCommand(ClanAdminContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("disband")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-clans disband <clan_name>")))
                .then(new StringArgument("clan")
                        .executes((CommandSender sender, CommandArguments args) -> {
                            String name = (String) args.get("clan");
                            ctx.clanService().getClanByName(name).thenCompose(opt -> {
                                if (opt.isEmpty()) {
                                    sender.sendMessage(Component.text("No clan named '" + name + "' found.", NamedTextColor.RED));
                                    return CompletableFuture.completedFuture(null);
                                }
                                return ctx.clanService().disbandClan(opt.get().clanId())
                                        .thenRun(() -> sender.sendMessage(
                                                MINI.deserialize("<yellow>Force-disbanded clan <white>" + name + "<yellow>.")));
                            }).exceptionally(ex -> { sender.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                        }));
    }
}
