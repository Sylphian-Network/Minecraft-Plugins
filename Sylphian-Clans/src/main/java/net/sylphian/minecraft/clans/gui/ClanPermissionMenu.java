package net.sylphian.minecraft.clans.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanMember;
import net.sylphian.minecraft.clans.model.ClanPermission;
import net.sylphian.minecraft.clans.model.ClanRole;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chest GUI for viewing and editing a single clan member's permissions.
 *
 * <p>Each capability occupies one slot: left-click toggles the capability,
 * right-click toggles its {@code GRANT_*} counterpart (leader only). The GUI is
 * a render snapshot read from {@link ClanCache}; every toggle is routed through
 * {@link ClanService}, which is the authority gate, and the menu rebuilds from
 * the freshly cached snapshot on success.</p>
 */
public final class ClanPermissionMenu {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

    private static final int SIZE = 54;
    private static final int HEAD_SLOT = 4;
    private static final int LEGEND_SLOT = 48;
    private static final int CLOSE_SLOT = 49;

    /** A capability icon definition: which permission, its material, label, and description. */
    private record Entry(ClanPermission permission, Material material, String title, String description) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry(ClanPermission.BREAK_BLOCKS, Material.IRON_PICKAXE, "Break Blocks", "Break blocks in clan territory."),
            new Entry(ClanPermission.PLACE_BLOCKS, Material.BRICKS, "Place Blocks", "Place blocks in clan territory."),
            new Entry(ClanPermission.KILL_ANIMALS, Material.PORKCHOP, "Kill Animals", "Kill passive animals in clan territory."),
            new Entry(ClanPermission.KILL_MONSTERS, Material.IRON_SWORD, "Kill Monsters", "Kill hostile monsters in clan territory."),
            new Entry(ClanPermission.OPEN_CONTAINERS, Material.CHEST, "Open Containers", "Open chests, barrels, and other containers."),
            new Entry(ClanPermission.USE_DOORS, Material.OAK_DOOR, "Use Doors", "Use doors, gates, and trapdoors."),
            new Entry(ClanPermission.INTERACT, Material.LEVER, "Interact", "Use buttons, levers, crafting tables, etc."),
            new Entry(ClanPermission.INVITE_MEMBERS, Material.WRITABLE_BOOK, "Invite Members", "Invite players to the clan."),
            new Entry(ClanPermission.KICK_MEMBERS, Material.IRON_BOOTS, "Kick Members", "Kick members from the clan."),
            new Entry(ClanPermission.CLAIM_TERRITORY, Material.FILLED_MAP, "Claim Territory", "Claim chunks for the clan."),
            new Entry(ClanPermission.UNCLAIM_TERRITORY, Material.MAP, "Unclaim Territory", "Unclaim the clan's chunks."),
            new Entry(ClanPermission.MANAGE_WARP, Material.ENDER_PEARL, "Manage Warps", "Create, remove, and manage clan warps."),
            new Entry(ClanPermission.SET_MOTD, Material.OAK_SIGN, "Set MOTD", "Set the clan's message of the day.")
    );

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24};

    private final ClanService clanService;
    private final ClanCache clanCache;
    private final JavaPlugin plugin;

    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public ClanPermissionMenu(ClanService clanService, ClanCache clanCache, JavaPlugin plugin) {
        this.clanService = clanService;
        this.clanCache = clanCache;
        this.plugin = plugin;
    }

    /**
     * Opens (or rebuilds) the permission GUI for {@code viewer} targeting {@code targetId}.
     * Validates the target is still a non-leader member of the viewer's clan; otherwise
     * closes any open inventory and messages the viewer. Must run on the main thread.
     *
     * @param viewer   the player to show the GUI to
     * @param targetId the member whose permissions are shown
     */
    public void open(Player viewer, UUID targetId) {
        Clan clan = clanCache.getOrNull(viewer.getUniqueId());
        if (clan == null) {
            viewer.closeInventory();
            viewer.sendMessage(Component.text("You are not in a clan.", NamedTextColor.RED));
            return;
        }
        Optional<ClanMember> targetOpt = clan.getMember(targetId);
        if (targetOpt.isEmpty()) {
            viewer.closeInventory();
            viewer.sendMessage(Component.text(resolveName(targetId) + " is no longer in your clan.", NamedTextColor.RED));
            return;
        }
        ClanMember target = targetOpt.get();
        if (target.role() == ClanRole.LEADER) {
            viewer.closeInventory();
            viewer.sendMessage(Component.text(resolveName(targetId) + " is the leader and has all permissions.", NamedTextColor.GRAY));
            return;
        }

        boolean viewerIsLeader = clan.leaderId().map(viewer.getUniqueId()::equals).orElse(false);
        String targetName = resolveName(targetId);

        Map<Integer, ClanPermission> slotToPermission = new HashMap<>();
        ClanPermissionHolder holder = new ClanPermissionHolder(this, viewer.getUniqueId(), targetId, clan.clanId(), slotToPermission);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, MINI.deserialize("<dark_aqua>Permissions: <white>" + targetName));

        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(HEAD_SLOT, head(target, targetName));
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);
            int slot = SLOTS[i];
            slotToPermission.put(slot, entry.permission());
            inventory.setItem(slot, icon(entry, target, clan, viewer.getUniqueId(), viewerIsLeader));
        }
        inventory.setItem(LEGEND_SLOT, legend());
        inventory.setItem(CLOSE_SLOT, new ItemBuilder(Material.BARRIER).name("<red>Close").build());

        viewer.openInventory(inventory);
    }

    /**
     * Toggles {@code permission} on the target, choosing grant or revoke from the current
     * snapshot, then rebuilds the GUI on success. Silently ignores clicks the viewer is not
     * authorised for (the lore already marks them locked); the service remains the final gate.
     *
     * @param viewer     the player who clicked
     * @param holder     the open GUI's holder
     * @param permission the permission to toggle (a base capability or its {@code GRANT_*} counterpart)
     */
    public void toggle(Player viewer, ClanPermissionHolder holder, ClanPermission permission) {
        Clan clan = clanCache.getOrNull(viewer.getUniqueId());
        if (clan == null) {
            viewer.closeInventory();
            return;
        }
        Optional<ClanMember> targetOpt = clan.getMember(holder.getTargetId());
        if (targetOpt.isEmpty()) {
            viewer.closeInventory();
            viewer.sendMessage(Component.text(resolveName(holder.getTargetId()) + " is no longer in your clan.", NamedTextColor.RED));
            return;
        }

        boolean viewerIsLeader = clan.leaderId().map(viewer.getUniqueId()::equals).orElse(false);
        boolean allowed = permission.isGrant()
                ? viewerIsLeader
                : viewerIsLeader || clan.hasPermission(viewer.getUniqueId(), permission.asGrant());
        if (!allowed) {
            return;
        }

        UUID viewerId = viewer.getUniqueId();
        if (!inFlight.add(viewerId)) {
            return;
        }

        boolean currentlyHas = targetOpt.get().permissions().contains(permission);
        UUID targetId = holder.getTargetId();
        CompletableFuture<Void> op = currentlyHas
                ? clanService.revokePermission(viewerId, targetId, permission)
                : clanService.grantPermission(viewerId, targetId, permission);

        op.whenComplete((result, ex) -> inFlight.remove(viewerId));
        op.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> open(viewer, targetId)))
                .exceptionally(ex -> {
                    viewer.sendMessage(Component.text(rootCause(ex), NamedTextColor.RED));
                    return null;
                });
    }

    private ItemStack icon(Entry entry, ClanMember target, Clan clan, UUID viewerId, boolean viewerIsLeader) {
        ClanPermission base = entry.permission();
        ClanPermission grant = base.asGrant();

        boolean hasBase = target.permissions().contains(base);
        boolean hasGrant = target.permissions().contains(grant);
        boolean canEditBase = viewerIsLeader || clan.hasPermission(viewerId, grant);

        String name = (hasBase ? "<green>✔ " : "<red>✘ ") + entry.title();

        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + entry.description());
        lore.add("");
        lore.add("<gray>Capability: " + (hasBase ? "<green>Granted" : "<red>Not granted"));
        lore.add(canEditBase
                ? "<yellow>Left-click to " + (hasBase ? "revoke" : "grant")
                : "<dark_gray>Locked · needs " + grant.name());
        lore.add("");
        lore.add("<gray>Can grant to others: " + (hasGrant ? "<green>Yes" : "<red>No"));
        lore.add(viewerIsLeader
                ? "<yellow>Right-click to " + (hasGrant ? "revoke" : "grant")
                : "<dark_gray>Locked · leader only");

        ItemBuilder builder = new ItemBuilder(entry.material()).name(name).loreStrings(lore).hideAttributes();
        if (hasBase) {
            builder.glint();
        }
        return builder.build();
    }

    private ItemStack head(ClanMember target, String targetName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(target.playerId()));
        meta.displayName(strip(MINI.deserialize("<white>" + targetName)));
        meta.lore(List.of(
                strip(MINI.deserialize("<gray>Role: <white>" + target.role().name())),
                strip(MINI.deserialize("<gray>Permissions: <white>" + target.permissions().size())),
                strip(MINI.deserialize("<gray>Joined: <white>" + DATE_FMT.format(target.joinedAt())))
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack legend() {
        return new ItemBuilder(Material.BOOK)
                .name("<aqua>How to use")
                .lore(
                        "<green>✔ <gray>granted   <red>✘ <gray>not granted",
                        "",
                        "<yellow>Left-click <gray>toggle the capability",
                        "<yellow>Right-click <gray>toggle who can grant it",
                        "<dark_gray>Locked actions need the right authority."
                )
                .build();
    }

    private Component strip(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private String resolveName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString();
    }

    private String rootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : "An error occurred.";
    }
}
