package net.sylphian.minecraft.clans.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.db.models.ClanWarpModel;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import net.sylphian.minecraft.clans.service.ClanTeleportWarmupManager;
import net.sylphian.minecraft.clans.service.ClanWarpService;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chest GUI listing a clan's warps. Left-click a usable warp to teleport (with warmup);
 * right-click a warp to manage its access if you hold MANAGE_WARP.
 *
 * <p>The GUI is a render snapshot read from the database; usability is resolved in a single
 * batch query and teleports re-validate access before starting the warmup.</p>
 */
public final class ClanWarpMenu {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int SIZE = 54;
    private static final int MAX_WARP_SLOTS = 45;
    public static final int CLOSE_SLOT = 49;

    private final ClanWarpService warpService;
    private final ClanCache clanCache;
    private final ClanTeleportWarmupManager warmupManager;
    private final ClanWarpAccessMenu accessMenu;
    private final JavaPlugin plugin;

    public ClanWarpMenu(ClanWarpService warpService, ClanCache clanCache,
                        ClanTeleportWarmupManager warmupManager, ClanWarpAccessMenu accessMenu, JavaPlugin plugin) {
        this.warpService = warpService;
        this.clanCache = clanCache;
        this.warmupManager = warmupManager;
        this.accessMenu = accessMenu;
        this.plugin = plugin;
    }

    /**
     * Opens the warp list for the viewer's clan. Must be called on the main thread.
     *
     * @param viewer the player to show the GUI to
     */
    public void open(Player viewer) {
        Clan clan = clanCache.getOrNull(viewer.getUniqueId());
        if (clan == null) {
            viewer.closeInventory();
            viewer.sendMessage(Component.text("You are not in a clan.", NamedTextColor.RED));
            return;
        }
        UUID clanId = clan.clanId();
        UUID viewerId = viewer.getUniqueId();

        warpService.listWarps(clanId)
                .thenAcceptBoth(warpService.accessibleWarps(clanId, viewerId),
                        (warps, accessible) -> plugin.getServer().getScheduler().runTask(plugin,
                                () -> render(viewer, clan, warps, accessible)))
                .exceptionally(ex -> {
                    viewer.sendMessage(Component.text("Failed to load warps.", NamedTextColor.RED));
                    return null;
                });
    }

    private void render(Player viewer, Clan clan, List<ClanWarpModel> warps, Set<String> accessible) {
        boolean canManage = clan.hasPermission(viewer.getUniqueId(), ClanPermission.MANAGE_WARP);

        Map<Integer, ClanWarpModel> slotToWarp = new HashMap<>();
        Set<Integer> usableSlots = new HashSet<>();
        ClanWarpHolder holder = new ClanWarpHolder(this, viewer.getUniqueId(), clan.clanId(), canManage, slotToWarp, usableSlots);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, MINI.deserialize("<dark_aqua>Clan Warps"));

        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = MAX_WARP_SLOTS; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(CLOSE_SLOT, new ItemBuilder(Material.BARRIER).name("<red>Close").build());

        if (warps.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.MAP)
                    .name("<gray>No warps yet")
                    .lore("<dark_gray>A member with Manage Warps can", "<dark_gray>create one with /clan warp set.")
                    .build());
        }

        int slot = 0;
        for (ClanWarpModel warp : warps) {
            if (slot >= MAX_WARP_SLOTS) {
                break;
            }
            boolean usable = !warp.restricted() || canManage || accessible.contains(warp.name());
            slotToWarp.put(slot, warp);
            if (usable) {
                usableSlots.add(slot);
            }
            inventory.setItem(slot, warpIcon(warp, usable, canManage));
            slot++;
        }

        viewer.openInventory(inventory);
    }

    private ItemStack warpIcon(ClanWarpModel warp, boolean usable, boolean canManage) {
        Material material = Material.matchMaterial(warp.icon());
        if (material == null || !material.isItem()) {
            material = Material.ENDER_PEARL;
        }

        List<String> lore = new ArrayList<>();
        if (warp.description() != null && !warp.description().isBlank()) {
            lore.add("<gray>" + warp.description());
            lore.add("");
        }
        lore.add("<gray>Access: " + (warp.restricted() ? "<yellow>Restricted" : "<green>Public to clan"));
        lore.add(usable ? "<green>Left-click to teleport" : "<red>You don't have access to this warp");
        if (canManage) {
            lore.add("<yellow>Right-click to manage access");
        }

        return new ItemBuilder(material)
                .name("<aqua>" + warp.name())
                .loreStrings(lore)
                .build();
    }

    /**
     * Re-validates access and starts a warmup teleport to the warp. Called from the listener.
     *
     * @param viewer the player teleporting
     * @param warp   the destination warp
     */
    public void teleport(Player viewer, ClanWarpModel warp) {
        Clan clan = clanCache.getOrNull(viewer.getUniqueId());
        if (clan == null) {
            viewer.closeInventory();
            return;
        }
        warpService.canUse(clan, viewer.getUniqueId(), warp).thenAccept(can ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!can) {
                        viewer.sendMessage(Component.text("You don't have access to that warp.", NamedTextColor.RED));
                        return;
                    }
                    World world = Bukkit.getWorld(warp.world());
                    if (world == null) {
                        viewer.sendMessage(Component.text("The warp world '" + warp.world() + "' is not loaded.", NamedTextColor.RED));
                        return;
                    }
                    viewer.closeInventory();
                    Location dest = new Location(world, warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch());
                    warmupManager.start(viewer, dest, warp.name());
                })).exceptionally(ex -> {
            viewer.sendMessage(Component.text("Failed to teleport.", NamedTextColor.RED));
            return null;
        });
    }

    /**
     * Opens the per-warp access editor for the given warp.
     *
     * @param viewer the player managing the warp
     * @param warp   the warp to manage
     */
    public void openAccess(Player viewer, ClanWarpModel warp) {
        accessMenu.open(viewer, warp.name());
    }
}
