package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan map} — renders a 9x9 territory map around the player. */
public final class MapSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public MapSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("map")
                .executesPlayer((Player player, CommandArguments _) -> render(player));
    }

    private void render(Player player) {
        Chunk centre = player.getLocation().getChunk();
        String world = centre.getWorld().getName();
        int radius = 4; // 9x9 grid

        Set<UUID> clanIds = new HashSet<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dz == 0) continue;
                ctx.territoryService().getClaimingClan(world, centre.getX() + dx, centre.getZ() + dz)
                        .ifPresent(clanIds::add);
            }
        }

        List<CompletableFuture<Optional<Clan>>> futures = clanIds.stream()
                .map(ctx.clanService()::getClanById)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    Map<UUID, Clan> clanMap = new HashMap<>();
                    futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .forEach(cl -> clanMap.put(cl.clanId(), cl));

                    Clan playerClan = ctx.clanCache().get(player.getUniqueId()).orElse(null);

                    player.sendMessage(MINI.deserialize("<gray>Territory map (you are at <white>+<gray>):"));

                    for (int dz = -radius; dz <= radius; dz++) {
                        Component row = Component.empty();
                        for (int dx = -radius; dx <= radius; dx++) {
                            int cx = centre.getX() + dx;
                            int cz = centre.getZ() + dz;

                            Component cell;
                            if (dx == 0 && dz == 0) {
                                cell = Component.text("+", NamedTextColor.WHITE);
                            } else {
                                Optional<UUID> ownerOpt = ctx.territoryService().getClaimingClan(world, cx, cz);
                                if (ownerOpt.isEmpty()) {
                                    cell = Component.text("#", NamedTextColor.DARK_GRAY);
                                } else {
                                    UUID ownerId = ownerOpt.get();
                                    boolean isOwn = playerClan != null && playerClan.clanId().equals(ownerId);
                                    NamedTextColor colour = isOwn ? NamedTextColor.GREEN : NamedTextColor.RED;

                                    Clan owner = clanMap.get(ownerId);
                                    if (owner != null) {
                                        String leaderName = owner.leaderId()
                                                .map(ctx::resolvePlayerName)
                                                .orElse("Unknown");
                                        Component tooltip = Component.text()
                                                .append(Component.text(owner.name(), NamedTextColor.YELLOW))
                                                .appendNewline()
                                                .append(Component.text("Leader: " + leaderName, NamedTextColor.GRAY))
                                                .build();
                                        cell = Component.text("#", colour)
                                                .hoverEvent(HoverEvent.showText(tooltip));
                                    } else {
                                        cell = Component.text("#", colour);
                                    }
                                }
                            }
                            row = row.append(cell);
                        }
                        player.sendMessage(row);
                    }
                    player.sendMessage(MINI.deserialize(
                            "<gray><green>#<gray>=your clan  <red>#<gray>=other clan  <dark_gray>#<gray>=unclaimed"));
                })
                .exceptionally(ex -> {
                    player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED));
                    return null;
                });
    }
}
