package net.sylphian.minecraft.clans.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import static net.sylphian.minecraft.clans.command.admin.ClanAdminContext.MINI;

/** {@code /sylphian-clans unclaim <world> <chunk_x> <chunk_z>} — force-unclaims a specific chunk. */
public final class UnclaimSubCommand implements SubCommand {

    private final ClanAdminContext ctx;

    public UnclaimSubCommand(ClanAdminContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("unclaim")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-clans unclaim <world> <chunk_x> <chunk_z>")))
                .then(new StringArgument("world")
                        .replaceSuggestions(ArgumentSuggestions.strings(_ ->
                                Bukkit.getWorlds().stream().map(World::getName).toArray(String[]::new)))
                        .then(new IntegerArgument("chunk_x")
                                .then(new IntegerArgument("chunk_z")
                                        .executes((CommandSender sender, CommandArguments args) -> {
                                            String world = (String) args.get("world");
                                            int chunkX = (int) args.get("chunk_x");
                                            int chunkZ = (int) args.get("chunk_z");
                                            ctx.territoryService().getClaimingClan(world, chunkX, chunkZ).ifPresentOrElse(
                                                    clanId -> ctx.territoryService().unclaimChunk(clanId, world, chunkX, chunkZ)
                                                            .thenRun(() -> sender.sendMessage(MINI.deserialize(
                                                                    "<yellow>Force-unclaimed chunk [<white>" + chunkX + "<yellow>, <white>" + chunkZ + "<yellow>] in <white>" + world + "<yellow>.")))
                                                            .exceptionally(ex -> { sender.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; }),
                                                    () -> sender.sendMessage(Component.text("That chunk is not claimed.", NamedTextColor.GRAY))
                                            );
                                        }))));
    }
}
