package net.sylphian.minecraft.clans.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.db.models.ClanWarpModel;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanMember;
import net.sylphian.minecraft.clans.model.ClanPermission;
import net.sylphian.minecraft.clans.service.ClanWarpService;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-warp access editor: toggle a warp between public and restricted, and grant or revoke
 * individual members' access. Only meaningful to members with MANAGE_WARP.
 */
public final class ClanWarpAccessMenu {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int SIZE = 54;
    private static final int WARP_SLOT = 4;
    public static final int RESTRICT_SLOT = 8;
    public static final int CLOSE_SLOT = 49;
    private static final int FIRST_MEMBER_SLOT = 18;

    private final ClanWarpService warpService;
    private final ClanCache clanCache;
    private final JavaPlugin plugin;

    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public ClanWarpAccessMenu(ClanWarpService warpService, ClanCache clanCache, JavaPlugin plugin) {
        this.warpService = warpService;
        this.clanCache = clanCache;
        this.plugin = plugin;
    }

    /**
     * Opens (or rebuilds) the access editor for a warp. Must be called on the main thread.
     *
     * @param viewer   the managing player
     * @param warpName the warp to edit
     */
    public void open(Player viewer, String warpName) {
        Clan clan = clanCache.getOrNull(viewer.getUniqueId());
        if (clan == null) {
            viewer.closeInventory();
            viewer.sendMessage(Component.text("You are not in a clan.", NamedTextColor.RED));
            return;
        }
        if (!clan.hasPermission(viewer.getUniqueId(), ClanPermission.MANAGE_WARP)) {
            viewer.closeInventory();
            viewer.sendMessage(Component.text("You don't have permission to manage warps.", NamedTextColor.RED));
            return;
        }
        UUID clanId = clan.clanId();

        warpService.getWarp(clanId, warpName)
                .thenAcceptBoth(warpService.listAccess(clanId, warpName),
                        (warpOpt, accessList) -> plugin.getServer().getScheduler().runTask(plugin,
                                () -> render(viewer, clan, warpName, warpOpt, Set.copyOf(accessList))))
                .exceptionally(ex -> {
                    viewer.sendMessage(Component.text("Failed to load warp access.", NamedTextColor.RED));
                    return null;
                });
    }

    private void render(Player viewer, Clan clan, String warpName, Optional<ClanWarpModel> warpOpt, Set<UUID> accessSet) {
        if (warpOpt.isEmpty()) {
            viewer.closeInventory();
            viewer.sendMessage(Component.text("Warp '" + warpName + "' no longer exists.", NamedTextColor.RED));
            return;
        }
        ClanWarpModel warp = warpOpt.get();

        Map<Integer, UUID> slotToMember = new HashMap<>();
        ClanWarpAccessHolder holder = new ClanWarpAccessHolder(this, viewer.getUniqueId(), clan.clanId(), warpName, slotToMember);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, MINI.deserialize("<dark_aqua>Warp Access: <white>" + warpName));

        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < FIRST_MEMBER_SLOT; i++) {
            inventory.setItem(i, filler);
        }
        for (int i = SIZE - 9; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(WARP_SLOT, warpInfo(warp));
        inventory.setItem(RESTRICT_SLOT, restrictToggle(warp));
        inventory.setItem(CLOSE_SLOT, new ItemBuilder(Material.BARRIER).name("<red>Close").build());

        int slot = FIRST_MEMBER_SLOT;
        for (ClanMember member : clan.members()) {
            if (slot >= SIZE - 9) {
                break;
            }
            boolean implicit = clan.hasPermission(member.playerId(), ClanPermission.MANAGE_WARP);
            boolean hasAccess = implicit || accessSet.contains(member.playerId());
            if (!implicit) {
                slotToMember.put(slot, member.playerId());
            }
            inventory.setItem(slot, memberHead(member.playerId(), hasAccess, implicit, warp.restricted()));
            slot++;
        }

        viewer.openInventory(inventory);
    }

    /**
     * Toggles the restricted flag on the warp, then rebuilds. Called from the listener.
     */
    public void toggleRestricted(Player viewer, ClanWarpAccessHolder holder) {
        Clan clan = clanCache.getOrNull(viewer.getUniqueId());
        if (clan == null || !clan.hasPermission(viewer.getUniqueId(), ClanPermission.MANAGE_WARP)) {
            viewer.closeInventory();
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        if (!inFlight.add(viewerId)) {
            return;
        }
        UUID clanId = holder.getClanId();
        String warpName = holder.getWarpName();
        warpService.getWarp(clanId, warpName)
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        return java.util.concurrent.CompletableFuture.<Void>completedFuture(null);
                    }
                    return warpService.setRestricted(clanId, warpName, !opt.get().restricted());
                })
                .whenComplete((v, ex) -> inFlight.remove(viewerId))
                .thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> open(viewer, warpName)))
                .exceptionally(ex -> {
                    viewer.sendMessage(Component.text("Failed to update the warp.", NamedTextColor.RED));
                    return null;
                });
    }

    /**
     * Toggles a member's access to the warp, then rebuilds. Called from the listener.
     */
    public void toggleAccess(Player viewer, ClanWarpAccessHolder holder, UUID memberId) {
        Clan clan = clanCache.getOrNull(viewer.getUniqueId());
        if (clan == null || !clan.hasPermission(viewer.getUniqueId(), ClanPermission.MANAGE_WARP)) {
            viewer.closeInventory();
            return;
        }
        if (clan.hasPermission(memberId, ClanPermission.MANAGE_WARP)) {
            return; // implicit access; nothing to toggle
        }
        UUID viewerId = viewer.getUniqueId();
        if (!inFlight.add(viewerId)) {
            return;
        }
        UUID clanId = holder.getClanId();
        String warpName = holder.getWarpName();
        warpService.listAccess(clanId, warpName)
                .thenCompose(access -> access.contains(memberId)
                        ? warpService.revokeAccess(clanId, warpName, memberId)
                        : warpService.grantAccess(clanId, warpName, memberId))
                .whenComplete((v, ex) -> inFlight.remove(viewerId))
                .thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> open(viewer, warpName)))
                .exceptionally(ex -> {
                    viewer.sendMessage(Component.text("Failed to update access.", NamedTextColor.RED));
                    return null;
                });
    }

    private ItemStack warpInfo(ClanWarpModel warp) {
        Material material = Material.matchMaterial(warp.icon());
        if (material == null || !material.isItem()) {
            material = Material.ENDER_PEARL;
        }
        ItemBuilder builder = new ItemBuilder(material).name("<aqua>" + warp.name());
        if (warp.description() != null && !warp.description().isBlank()) {
            builder.lore("<gray>" + warp.description());
        }
        return builder.build();
    }

    private ItemStack restrictToggle(ClanWarpModel warp) {
        boolean restricted = warp.restricted();
        return new ItemBuilder(restricted ? Material.RED_DYE : Material.LIME_DYE)
                .name(restricted ? "<yellow>Restricted" : "<green>Public to clan")
                .lore(
                        restricted
                                ? "<gray>Only the leader, Manage Warps holders,"
                                : "<gray>Any clan member can use this warp.",
                        restricted ? "<gray>and granted members can use this warp." : "",
                        "",
                        "<yellow>Click to make it " + (restricted ? "public" : "restricted")
                )
                .build();
    }

    private ItemStack memberHead(UUID memberId, boolean hasAccess, boolean implicit, boolean restricted) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(memberId));
        meta.displayName(strip(MINI.deserialize((hasAccess ? "<green>" : "<red>") + resolveName(memberId))));

        String statusLine;
        if (implicit) {
            statusLine = "<gray>Access: <gold>Always (Manage Warps)";
        } else if (!restricted) {
            statusLine = "<gray>Access: <green>Public warp";
        } else {
            statusLine = "<gray>Access: " + (hasAccess ? "<green>Granted" : "<red>Not granted");
        }

        List<Component> lore = new java.util.ArrayList<>();
        lore.add(strip(MINI.deserialize(statusLine)));
        if (!implicit) {
            lore.add(strip(MINI.deserialize("<yellow>Click to " + (hasAccess ? "revoke" : "grant"))));
            if (!restricted) {
                lore.add(strip(MINI.deserialize("<dark_gray>(Only matters once the warp is restricted)")));
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
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
}
