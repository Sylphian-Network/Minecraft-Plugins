package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** {@code /clan home} — starts a warmup teleport to the clan home. */
public final class HomeSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public HomeSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("home")
                .executesPlayer((Player player, CommandArguments _) -> {
                    Clan clan = ctx.requireClan(player);
                    if (clan == null) {
                        return;
                    }
                    ctx.clanService().getHome(clan.clanId()).thenAccept(opt -> {
                        if (opt.isEmpty()) {
                            player.sendMessage(Component.text(
                                    "Your clan has no home set. A leader can use /clan sethome.", NamedTextColor.RED));
                            return;
                        }
                        var model = opt.get();
                        var world = Bukkit.getWorld(model.world());
                        if (world == null) {
                            player.sendMessage(Component.text(
                                    "The clan home world '" + model.world() + "' is not loaded.", NamedTextColor.RED));
                            return;
                        }
                        var dest = new Location(world, model.x(), model.y(), model.z(), model.yaw(), model.pitch());
                        ctx.warmupManager().start(player, dest);
                    }).exceptionally(ex -> {
                        player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED));
                        return null;
                    });
                });
    }
}
