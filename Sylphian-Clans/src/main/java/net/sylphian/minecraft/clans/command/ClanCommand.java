package net.sylphian.minecraft.clans.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanMember;
import net.sylphian.minecraft.clans.model.ClanPermission;
import net.sylphian.minecraft.clans.model.ClanRole;
import net.sylphian.minecraft.clans.service.ClanHomeWarmupManager;
import net.sylphian.minecraft.clans.service.ClanInviteService;
import net.sylphian.minecraft.clans.service.ClanInviteService.PendingInvite;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Root {@code /clan} command with all player-facing subcommands.
 */
public class ClanCommand implements BasicCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

    private final ClanService clanService;
    private final ClanInviteService inviteService;
    private final TerritoryService territoryService;
    private final ClanCache clanCache;
    private final ClanHomeWarmupManager warmupManager;

    /**
     * @param clanService      the clan business logic service
     * @param inviteService    the in-memory invite store
     * @param territoryService the territory claiming service
     * @param clanCache        the in-memory membership cache
     * @param warmupManager    manages pending home teleport warmups
     */
    public ClanCommand(ClanService clanService, ClanInviteService inviteService,
                       TerritoryService territoryService, ClanCache clanCache,
                       ClanHomeWarmupManager warmupManager) {
        this.clanService = clanService;
        this.inviteService = inviteService;
        this.territoryService = territoryService;
        this.clanCache = clanCache;
        this.warmupManager = warmupManager;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, @NonNull String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use clan commands.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) { sendUsage(player); return; }

        switch (args[0].toLowerCase()) {
            case "create"     -> handleCreate(player, args);
            case "disband"    -> handleDisband(player);
            case "invite"     -> handleInvite(player, args);
            case "accept"     -> handleAccept(player, args);
            case "decline"    -> handleDecline(player, args);
            case "invites"    -> handleInvites(player);
            case "leave"      -> handleLeave(player);
            case "kick"       -> handleKick(player, args);
            case "transfer"   -> handleTransfer(player, args);
            case "permission" -> handlePermission(player, args);
            case "claim"      -> handleClaim(player, args);
            case "unclaim"    -> handleUnclaim(player, args);
            case "map"        -> handleMap(player);
            case "info"       -> handleInfo(player, args);
            case "list"       -> handleList(player);
            case "sethome"    -> handleSetHome(player);
            case "home"       -> handleHome(player);
            case "delhome"    -> handleDelHome(player);
            default           -> sendUsage(player);
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MINI.deserialize("<red>Usage: /clan create <name>"));
            return;
        }
        if (clanCache.get(player.getUniqueId()).isPresent()) {
            player.sendMessage(Component.text("You are already in a clan.", NamedTextColor.RED));
            return;
        }
        String name = args[1];
        clanService.createClan(player.getUniqueId(), name)
                .thenRun(() -> player.sendMessage(MINI.deserialize("<green>Clan <white>" + name + " <green>created!")))
                .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void handleDisband(Player player) {
        Clan clan = requireLeader(player);
        if (clan == null) return;
        clanService.disbandClan(clan.clanId())
                .thenRun(() -> player.sendMessage(MINI.deserialize("<red>Clan <white>" + clan.name() + " <red>has been disbanded.")))
                .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MINI.deserialize("<red>Usage: /clan invite <player>")); return; }

        Clan clan = requirePermission(player, ClanPermission.INVITE_MEMBERS);
        if (clan == null) return;

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player '" + args[1] + "' is not online.", NamedTextColor.RED));
            return;
        }
        if (clan.isMember(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is already in your clan.", NamedTextColor.RED));
            return;
        }
        if (clanCache.get(target.getUniqueId()).isPresent()) {
            player.sendMessage(Component.text(target.getName() + " is already in another clan.", NamedTextColor.RED));
            return;
        }

        boolean added = inviteService.addInvite(clan.clanId(), clan.name(), player.getUniqueId(), target.getUniqueId());
        if (!added) {
            player.sendMessage(Component.text(target.getName() + " already has a pending invite from your clan.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(MINI.deserialize("<green>Invited <white>" + target.getName() + " <green>to your clan."));

        Component acceptButton = Component.text(" [Accept]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/clan accept " + clan.name()))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to join " + clan.name(), NamedTextColor.YELLOW)));

        target.sendMessage(Component.text()
                .append(MINI.deserialize("<green>You have been invited to join <white>" + clan.name()
                        + " <green>by <white>" + player.getName() + "<green>."))
                .append(acceptButton)
                .build());
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MINI.deserialize("<red>Usage: /clan accept <clan_name>")); return; }
        if (clanCache.get(player.getUniqueId()).isPresent()) {
            player.sendMessage(Component.text("You are already in a clan.", NamedTextColor.RED));
            return;
        }

        String clanName = args[1];
        inviteService.consumeInvite(player.getUniqueId(), clanName).ifPresentOrElse(
                invite -> clanService.addMember(invite.clanId(), player.getUniqueId(), inviteService)
                        .thenRun(() -> player.sendMessage(MINI.deserialize("<green>You joined <white>" + clanName + "<green>!")))
                        .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; }),
                () -> player.sendMessage(Component.text(
                        "No active invite found from '" + clanName + "'.", NamedTextColor.RED))
        );
    }

    private void handleDecline(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MINI.deserialize("<red>Usage: /clan decline <clan_name>")); return; }
        String clanName = args[1];
        inviteService.consumeInvite(player.getUniqueId(), clanName).ifPresentOrElse(
                invite -> player.sendMessage(MINI.deserialize("<yellow>Declined invite from <white>" + clanName + "<yellow>.")),
                () -> player.sendMessage(Component.text("No active invite found from '" + clanName + "'.", NamedTextColor.RED))
        );
    }

    private void handleInvites(Player player) {
        List<PendingInvite> invites = inviteService.getPendingInvites(player.getUniqueId());
        if (invites.isEmpty()) {
            player.sendMessage(Component.text("You have no pending clan invites.", NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(MINI.deserialize("<yellow>Pending invites:"));
        invites.forEach(i -> player.sendMessage(MINI.deserialize(
                "<gray>  - <white>" + i.clanName() + " <gray>(invited by <white>" +
                resolvePlayerName(i.inviterUuid()) + "<gray>)")));
    }

    private void handleLeave(Player player) {
        Clan clan = clanCache.get(player.getUniqueId()).orElse(null);
        if (clan == null) { player.sendMessage(Component.text("You are not in a clan.", NamedTextColor.RED)); return; }

        if (clan.leaderId().map(player.getUniqueId()::equals).orElse(false)) {
            if (clan.members().size() > 1) {
                player.sendMessage(Component.text(
                        "You are the leader. Transfer leadership first (/clan transfer <player>) or disband (/clan disband).",
                        NamedTextColor.RED));
                return;
            }

            // Leader is the sole member, disband.
            clanService.disbandClan(clan.clanId())
                    .thenRun(() -> player.sendMessage(MINI.deserialize("<red>You were the last member. Clan disbanded.")))
                    .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
            return;
        }

        clanService.removeMember(clan.clanId(), player.getUniqueId())
                .thenRun(() -> player.sendMessage(MINI.deserialize("<yellow>You left <white>" + clan.name() + "<yellow>.")))
                .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MINI.deserialize("<red>Usage: /clan kick <player>")); return; }

        Clan clan = requirePermission(player, ClanPermission.KICK_MEMBERS);
        if (clan == null) return;

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player '" + args[1] + "' is not online.", NamedTextColor.RED));
            return;
        }
        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is not in your clan.", NamedTextColor.RED));
            return;
        }
        if (clan.leaderId().map(target.getUniqueId()::equals).orElse(false)) {
            player.sendMessage(Component.text("You cannot kick the clan leader.", NamedTextColor.RED));
            return;
        }

        clanService.removeMember(clan.clanId(), target.getUniqueId())
                .thenRun(() -> {
                    player.sendMessage(MINI.deserialize("<yellow>Kicked <white>" + target.getName() + "<yellow> from the clan."));
                    target.sendMessage(MINI.deserialize("<red>You were kicked from <white>" + clan.name() + "<red>."));
                })
                .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MINI.deserialize("<red>Usage: /clan transfer <player>")); return; }

        Clan clan = requireLeader(player);
        if (clan == null) return;

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player '" + args[1] + "' is not online.", NamedTextColor.RED));
            return;
        }
        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is not in your clan.", NamedTextColor.RED));
            return;
        }

        clanService.transferLeadership(clan.clanId(), target.getUniqueId())
                .thenRun(() -> {
                    player.sendMessage(MINI.deserialize("<yellow>Leadership transferred to <white>" + target.getName() + "<yellow>."));
                    target.sendMessage(MINI.deserialize("<green>You are now the leader of <white>" + clan.name() + "<green>!"));
                })
                .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void handlePermission(Player player, String[] args) {
        // /clan permission <player> grant|revoke <permission>
        // /clan permission <player> list
        if (args.length < 3) {
            player.sendMessage(MINI.deserialize("<red>Usage: /clan permission <player> grant|revoke|list <permission>"));
            return;
        }

        Clan clan = clanCache.get(player.getUniqueId()).orElse(null);
        if (clan == null) { player.sendMessage(Component.text("You are not in a clan.", NamedTextColor.RED)); return; }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player '" + args[1] + "' is not online.", NamedTextColor.RED));
            return;
        }
        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is not in your clan.", NamedTextColor.RED));
            return;
        }

        String action = args[2].toLowerCase();

        if (action.equals("list")) {
            ClanMember targetMember = clan.getMember(target.getUniqueId()).orElseThrow();
            if (targetMember.role() == ClanRole.LEADER) {
                player.sendMessage(MINI.deserialize("<gray>" + target.getName() + " is the LEADER and has all permissions."));
            } else {
                String perms = targetMember.permissions().stream()
                        .map(ClanPermission::name)
                        .sorted()
                        .collect(Collectors.joining(", "));
                player.sendMessage(MINI.deserialize("<gray>Permissions for <white>" + target.getName() + "<gray>: <white>"
                        + (perms.isEmpty() ? "none" : perms)));
            }
            return;
        }

        if (args.length < 4) {
            player.sendMessage(MINI.deserialize("<red>Usage: /clan permission <player> grant|revoke <permission>"));
            return;
        }

        ClanPermission permission;
        try {
            permission = ClanPermission.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Unknown permission: " + args[3], NamedTextColor.RED));
            return;
        }

        if (action.equals("grant")) {
            clanService.grantPermission(player.getUniqueId(), target.getUniqueId(), permission)
                    .thenRun(() -> {
                        player.sendMessage(MINI.deserialize("<green>Granted <white>" + permission.name() + " <green>to <white>" + target.getName() + "<green>."));
                        target.sendMessage(MINI.deserialize("<green>You were granted <white>" + permission.name() + " <green>in your clan."));
                    })
                    .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
        } else if (action.equals("revoke")) {
            clanService.revokePermission(player.getUniqueId(), target.getUniqueId(), permission)
                    .thenRun(() -> {
                        player.sendMessage(MINI.deserialize("<yellow>Revoked <white>" + permission.name() + " <yellow>from <white>" + target.getName() + "<yellow>."));
                        target.sendMessage(MINI.deserialize("<yellow>Your <white>" + permission.name() + " <yellow>permission was revoked."));
                    })
                    .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
        } else {
            player.sendMessage(MINI.deserialize("<red>Usage: /clan permission <player> grant|revoke|list <permission>"));
        }
    }

    private void handleClaim(Player player, String[] args) {
        Clan clan = requirePermission(player, ClanPermission.CLAIM_TERRITORY);
        if (clan == null) return;

        int radius = 0;
        if (args.length >= 2) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid radius: " + args[1], NamedTextColor.RED));
                return;
            }
            if (radius < 0 || radius > 5) {
                player.sendMessage(Component.text("Radius must be between 0 and 5.", NamedTextColor.RED));
                return;
            }
        }

        Chunk centre = player.getLocation().getChunk();
        String world = centre.getWorld().getName();

        List<int[]> chunks = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                chunks.add(new int[]{centre.getX() + dx, centre.getZ() + dz});
            }
        }

        List<String> failed = new ArrayList<>();
        CompletableFuture<Void> chain =
                CompletableFuture.completedFuture(null);

        for (int[] c : chunks) {
            int cx = c[0], cz = c[1];
            chain = chain.thenCompose(v ->
                    territoryService.claimChunk(clan.clanId(), world, cx, cz)
                            .exceptionally(ex -> { failed.add(cx + "," + cz); return null; }));
        }

        int total = chunks.size();
        chain.thenRun(() -> {
            int claimed = total - failed.size();
            if (claimed == 0) {
                player.sendMessage(Component.text(
                        "No chunks were claimed. They may already be taken or you have hit the limit.",
                        NamedTextColor.RED));
            } else if (failed.isEmpty()) {
                player.sendMessage(MINI.deserialize(
                        "<green>Claimed <white>" + claimed + " <green>chunk"
                                + (claimed == 1 ? "" : "s") + " for <white>" + clan.name() + "<green>."));
            } else {
                player.sendMessage(MINI.deserialize(
                        "<yellow>Claimed <white>" + claimed + "<yellow>/<white>" + total
                                + "<yellow> chunks. " + failed.size() + " already taken or at the limit."));
            }
        }).exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void handleUnclaim(Player player, String[] args) {
        Clan clan = requirePermission(player, ClanPermission.UNCLAIM_TERRITORY);
        if (clan == null) return;

        // /clan unclaim all
        if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
            if (!clan.leaderId().map(player.getUniqueId()::equals).orElse(false)) {
                player.sendMessage(Component.text("Only the clan leader can unclaim all territory.", NamedTextColor.RED));
                return;
            }
            territoryService.unclaimAll(clan.clanId())
                    .thenRun(() -> player.sendMessage(MINI.deserialize("<yellow>All territory unclaimed.")))
                    .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        territoryService.unclaimChunk(clan.clanId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ())
                .thenRun(() -> player.sendMessage(MINI.deserialize(
                        "<yellow>Chunk [<white>" + chunk.getX() + "<yellow>, <white>" + chunk.getZ() + "<yellow>] unclaimed.")))
                .exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void handleMap(Player player) {
        Chunk centre = player.getLocation().getChunk();
        String world = centre.getWorld().getName();
        int radius = 4; // 9×9 grid

        Set<UUID> clanIds = new HashSet<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dz == 0) continue;
                territoryService.getClaimingClan(world, centre.getX() + dx, centre.getZ() + dz)
                        .ifPresent(clanIds::add);
            }
        }

        List<CompletableFuture<Optional<Clan>>> futures = clanIds.stream()
                .map(clanService::getClanById)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    Map<UUID, Clan> clanMap = new HashMap<>();
                    futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .forEach(c -> clanMap.put(c.clanId(), c));

                    Clan playerClan = clanCache.get(player.getUniqueId()).orElse(null);

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
                                Optional<UUID> ownerOpt = territoryService.getClaimingClan(world, cx, cz);
                                if (ownerOpt.isEmpty()) {
                                    cell = Component.text("#", NamedTextColor.DARK_GRAY);
                                } else {
                                    UUID ownerId = ownerOpt.get();
                                    boolean isOwn = playerClan != null && playerClan.clanId().equals(ownerId);
                                    NamedTextColor colour = isOwn ? NamedTextColor.GREEN : NamedTextColor.RED;

                                    Clan owner = clanMap.get(ownerId);
                                    if (owner != null) {
                                        String leaderName = owner.leaderId()
                                                .map(this::resolvePlayerName)
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
                    player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED));
                    return null;
                });
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            // Info for own clan.
            clanCache.get(player.getUniqueId()).ifPresentOrElse(
                    clan -> printClanInfo(player, clan),
                    () -> player.sendMessage(Component.text("You are not in a clan. Use /clan info <name> to look up another clan.", NamedTextColor.GRAY))
            );
            return;
        }

        String name = args[1];
        clanService.getClanByName(name).thenAccept(opt ->
                opt.ifPresentOrElse(
                        clan -> printClanInfo(player, clan),
                        () -> player.sendMessage(Component.text("No clan named '" + name + "' found.", NamedTextColor.RED))
                )
        ).exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void printClanInfo(Player player, Clan clan) {
        player.sendMessage(MINI.deserialize("<yellow>--- <white>" + clan.name() + " <yellow>---"));
        player.sendMessage(MINI.deserialize("<gray>Founded: <white>" + DATE_FMT.format(clan.createdAt())));
        player.sendMessage(MINI.deserialize("<gray>Members: <white>" + clan.members().size()));
        clan.members().forEach(m -> {
            String roleBadge = m.role() == ClanRole.LEADER ? "<gold>[L]</gold> " : "<gray>[M]</gray> ";
            player.sendMessage(MINI.deserialize("  " + roleBadge + "<white>" + resolvePlayerName(m.playerId())));
        });
    }

    private void handleList(Player player) {
        clanService.getAllClans().thenAccept(clans -> {
            if (clans.isEmpty()) {
                player.sendMessage(Component.text("No clans exist yet.", NamedTextColor.GRAY));
                return;
            }
            player.sendMessage(MINI.deserialize("<yellow>--- Clans (" + clans.size() + ") ---"));
            clans.forEach(c -> player.sendMessage(MINI.deserialize(
                    "<white>" + c.name() + " <gray>- " + c.members().size() + " members")));
        }).exceptionally(ex -> { player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED)); return null; });
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, @NonNull String[] args) {
        if (args.length <= 1) {
            if (!(source.getSender() instanceof Player player)) {
                return List.of();
            }
            String prefix = args.length == 1 ? args[0].toLowerCase() : "";
            UUID uuid = player.getUniqueId();

            Optional<Clan> clanOpt = clanCache.get(uuid);
            boolean inClan = clanOpt.isPresent();
            Clan clan = clanOpt.orElse(null);
            boolean leader = inClan && clan.leaderId().map(uuid::equals).orElse(false);
            boolean multiMember = inClan && clan.members().size() > 1;
            boolean hasInvites = !inviteService.getPendingInvites(uuid).isEmpty();

            List<String> options = new ArrayList<>();

            options.add("list");
            options.add("info");
            options.add("map");

            if (!inClan) {
                options.add("create");
                if (hasInvites) {
                    options.add("invites");
                    options.add("accept");
                    options.add("decline");
                }
            } else {
                options.add("home");
                if (!leader) options.add("leave");

                if (clan.hasPermission(uuid, ClanPermission.INVITE_MEMBERS))            options.add("invite");
                if (multiMember && clan.hasPermission(uuid, ClanPermission.KICK_MEMBERS)) options.add("kick");
                if (clan.hasPermission(uuid, ClanPermission.CLAIM_TERRITORY))           options.add("claim");
                if (clan.hasPermission(uuid, ClanPermission.UNCLAIM_TERRITORY))         options.add("unclaim");
                if (clan.hasPermission(uuid, ClanPermission.SET_HOME)) {
                    options.add("sethome");
                    options.add("delhome");
                }
                if (canManagePermissions(clan, uuid)) options.add("permission");

                if (leader) {
                    if (multiMember) options.add("transfer");
                    options.add("disband");
                }
            }

            return options.stream()
                    .filter(o -> o.startsWith(prefix))
                    .sorted()
                    .toList();
        }

        return switch (args[0].toLowerCase()) {
            case "invite", "kick", "transfer" -> onlinePlayers(args.length > 1 ? args[1] : "");
            case "accept", "decline" -> {
                if (!(source.getSender() instanceof Player player)) yield List.of();
                yield inviteService.getPendingInvites(player.getUniqueId()).stream()
                        .map(PendingInvite::clanName).toList();
            }
            case "unclaim" -> args.length == 2 ? List.of("all") : List.of();
            case "permission" -> {
                if (args.length == 2) yield onlinePlayers(args[1]);
                if (args.length == 3) yield List.of("grant", "revoke", "list");
                if (args.length == 4) yield Arrays.stream(ClanPermission.values())
                        .map(ClanPermission::name)
                        .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                        .toList();
                yield List.of();
            }
            default -> List.of();
        };
    }

    @Override
    public @NonNull String permission() { return "sylphian.clans.use"; }

    /**
     * Returns the player's clan if they are the LEADER, or sends an error and returns {@code null}.
     */
    private Clan requireLeader(Player player) {
        Clan clan = clanCache.get(player.getUniqueId()).orElse(null);
        if (clan == null) {
            player.sendMessage(Component.text("You are not in a clan.", NamedTextColor.RED));
            return null;
        }
        if (!clan.leaderId().map(player.getUniqueId()::equals).orElse(false)) {
            player.sendMessage(Component.text("Only the clan leader can do that.", NamedTextColor.RED));
            return null;
        }
        return clan;
    }

    /**
     * Returns the player's clan if they hold the given permission (or are LEADER),
     * or sends an error and returns {@code null}.
     */
    private Clan requirePermission(Player player, ClanPermission permission) {
        Clan clan = clanCache.get(player.getUniqueId()).orElse(null);
        if (clan == null) {
            player.sendMessage(Component.text("You are not in a clan.", NamedTextColor.RED));
            return null;
        }
        if (!clan.hasPermission(player.getUniqueId(), permission)) {
            player.sendMessage(Component.text("You don't have the " + permission.name() + " permission.", NamedTextColor.RED));
            return null;
        }
        return clan;
    }

    /**
     * @return true if the player can manage clan permissions (holds any {@code GRANT_*}
     * permission). The LEADER passes every permission check, so this covers leaders too.
     */
    private boolean canManagePermissions(Clan clan, UUID uuid) {
        return Arrays.stream(ClanPermission.values())
                .filter(p -> p.name().startsWith("GRANT_"))
                .anyMatch(p -> clan.hasPermission(uuid, p));
    }

    private void handleSetHome(Player player) {
        Clan clan = requirePermission(player, ClanPermission.SET_HOME);
        if (clan == null) return;

        clanService.setHome(clan.clanId(), player.getLocation())
                .thenRun(() -> player.sendMessage(
                        MINI.deserialize("<green>Clan home set to your current location.")))
                .exceptionally(ex -> {
                    player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED));
                    return null;
                });
    }

    private void handleHome(Player player) {
        Clan clan = clanCache.get(player.getUniqueId()).orElse(null);
        if (clan == null) {
            player.sendMessage(Component.text("You are not in a clan.", NamedTextColor.RED));
            return;
        }

        clanService.getHome(clan.clanId()).thenAccept(opt -> {
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
            var dest = new org.bukkit.Location(world, model.x(), model.y(), model.z(), model.yaw(), model.pitch());
            warmupManager.start(player, dest);
        }).exceptionally(ex -> {
            player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED));
            return null;
        });
    }

    private void handleDelHome(Player player) {
        Clan clan = requirePermission(player, ClanPermission.SET_HOME);
        if (clan == null) return;

        clanService.deleteHome(clan.clanId())
                .thenRun(() -> player.sendMessage(
                        MINI.deserialize("<yellow>Clan home removed.")))
                .exceptionally(ex -> {
                    player.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED));
                    return null;
                });
    }

    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }

    /** Resolves a player's name from their UUID, falling back to the UUID string if offline. */
    private String resolvePlayerName(java.util.UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        var profile = Bukkit.getOfflinePlayer(uuid);
        return profile.getName() != null ? profile.getName() : uuid.toString();
    }

    private String rootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : "An error occurred.";
    }

    private void sendUsage(Player player) {
        player.sendMessage(MINI.deserialize("""
                <yellow>--- /clan commands ---
                <white>/clan create <name>
                /clan disband
                /clan invite <player>
                /clan accept <clan>
                /clan decline <clan>
                /clan invites
                /clan leave
                /clan kick <player>
                /clan transfer <player>
                /clan permission <player> grant|revoke|list [perm]
                /clan claim [radius]
                /clan unclaim [all]
                /clan map
                /clan sethome
                /clan home
                /clan delhome
                /clan info [clan]
                /clan list"""));
    }
}
