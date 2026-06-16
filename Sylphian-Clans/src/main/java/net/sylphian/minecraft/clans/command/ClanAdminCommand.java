package net.sylphian.minecraft.clans.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;

/**
 * {@code /sylphian-clans} operator-only clan management commands.
 *
 * <p>Provides force-disbandment, force-kick, and force-unclaim operations
 * that bypass normal permission checks. Requires {@code sylphian.clans.admin}.</p>
 */
public class ClanAdminCommand implements BasicCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ClanService clanService;
    private final TerritoryService territoryService;

    /**
     * @param clanService      the clan service for disbandment and kick operations
     * @param territoryService the territory service for unclaim operations
     */
    public ClanAdminCommand(ClanService clanService, TerritoryService territoryService) {
        this.clanService = clanService;
        this.territoryService = territoryService;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, @NonNull String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) { sendUsage(sender); return; }

        switch (args[0].toLowerCase()) {
            case "disband" -> handleForceDisband(sender, args);
            case "kick"    -> handleForceKick(sender, args);
            case "unclaim" -> handleForceUnclaim(sender, args);
            default        -> sendUsage(sender);
        }
    }

    private void handleForceDisband(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-clans disband <clan_name>")); return; }

        String name = args[1];
        clanService.getClanByName(name).thenCompose(opt -> {
            if (opt.isEmpty()) {
                sender.sendMessage(Component.text("No clan named '" + name + "' found.", NamedTextColor.RED));
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            return clanService.disbandClan(opt.get().clanId())
                    .thenRun(() -> sender.sendMessage(
                            MINI.deserialize("<yellow>Force-disbanded clan <white>" + name + "<yellow>.")));
        }).exceptionally(ex -> { sender.sendMessage(Component.text(ex.getMessage() != null ? ex.getMessage() : "An error occurred.", NamedTextColor.RED)); return null; });
    }

    private void handleForceKick(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-clans kick <player>")); return; }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[1] + "' is not online.", NamedTextColor.RED));
            return;
        }

        clanService.getClanByPlayer(target.getUniqueId()).thenCompose(clanOpt -> {
            if (clanOpt.isEmpty()) {
                sender.sendMessage(Component.text(target.getName() + " is not in a clan.", NamedTextColor.RED));
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            return clanService.removeMember(clanOpt.get().clanId(), target.getUniqueId())
                    .thenRun(() -> {
                        sender.sendMessage(MINI.deserialize("<yellow>Force-kicked <white>" + target.getName() + "<yellow> from their clan."));
                        target.sendMessage(MINI.deserialize("<red>You were removed from your clan by an administrator."));
                    });
        }).exceptionally(ex -> { sender.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED)); return null; });
    }

    private void handleForceUnclaim(CommandSender sender, String[] args) {
        if (args.length < 4) { sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-clans unclaim <world> <chunk_x> <chunk_z>")); return; }

        String world = args[1];
        int chunkX, chunkZ;
        try {
            chunkX = Integer.parseInt(args[2]);
            chunkZ = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("chunk_x and chunk_z must be integers.", NamedTextColor.RED));
            return;
        }

        territoryService.getClaimingClan(world, chunkX, chunkZ).ifPresentOrElse(
                clanId -> territoryService.unclaimChunk(clanId, world, chunkX, chunkZ)
                        .thenRun(() -> sender.sendMessage(MINI.deserialize(
                                "<yellow>Force-unclaimed chunk [<white>" + chunkX + "<yellow>, <white>" + chunkZ + "<yellow>] in <white>" + world + "<yellow>.")))
                        .exceptionally(ex -> { sender.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED)); return null; }),
                () -> sender.sendMessage(Component.text("That chunk is not claimed.", NamedTextColor.GRAY))
        );
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, @NonNull String[] args) {
        if (args.length <= 1) return List.of("disband", "kick", "unclaim");
        if (args[0].equalsIgnoreCase("kick") && args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        if (args[0].equalsIgnoreCase("unclaim") && args.length == 2) {
            return Bukkit.getWorlds().stream().map(WorldInfo::getName).toList();
        }
        return List.of();
    }

    @Override
    public @NonNull String permission() { return "sylphian.clans.admin"; }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("""
                <yellow>--- /sylphian-clans commands ---
                <white>/sylphian-clans disband <clan_name>
                /sylphian-clans kick <player>
                /sylphian-clans unclaim <world> <chunk_x> <chunk_z>"""));
    }
}
