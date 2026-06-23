package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan create <name>} — founds a new clan with the sender as leader. */
public final class CreateSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public CreateSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("create")
                .executesPlayer((Player player, CommandArguments _) ->
                        player.sendMessage(MINI.deserialize("<red>Usage: /clan create <name>")))
                .then(new StringArgument("name")
                        .executesPlayer((Player player, CommandArguments args) -> {
                            if (ctx.clanCache().get(player.getUniqueId()).isPresent()) {
                                player.sendMessage(Component.text("You are already in a clan.", NamedTextColor.RED));
                                return;
                            }
                            String name = (String) args.get("name");
                            ctx.clanService().createClan(player.getUniqueId(), name)
                                    .thenRun(() -> player.sendMessage(MINI.deserialize("<green>Clan <white>" + name + " <green>created!")))
                                    .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                        }));
    }
}
