package net.sylphian.minecraft.clans.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Enforces territory protection in claimed chunks.
 *
 * <p>All checks are synchronous, non-blocking reads from {@link TerritoryService}
 * and {@link ClanCache}. Unclaimed chunks are always permitted.</p>
 *
 * <p>LEADER always bypasses every check. Members are tested against their personal
 * {@link ClanPermission} set. Non-members are always denied.</p>
 */
public class TerritoryProtectionListener implements Listener {

    private final TerritoryService territoryService;
    private final ClanCache clanCache;

    /**
     * @param territoryService used to look up the owning clan for a chunk
     * @param clanCache        used to retrieve the acting player's clan snapshot
     */
    public TerritoryProtectionListener(TerritoryService territoryService, ClanCache clanCache) {
        this.territoryService = territoryService;
        this.clanCache = clanCache;
    }

    /**
     * Prevents block breaking in claimed chunks by players who lack {@link ClanPermission#BREAK_BLOCKS}.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isDenied(event.getPlayer(), event.getBlock().getChunk(), ClanPermission.BREAK_BLOCKS)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    Component.text("You cannot break blocks in claimed territory.",
                            NamedTextColor.RED));
        }
    }

    /**
     * Prevents block placing in claimed chunks by players who lack {@link ClanPermission#PLACE_BLOCKS}.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isDenied(event.getPlayer(), event.getBlock().getChunk(), ClanPermission.PLACE_BLOCKS)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    Component.text("You cannot place blocks in claimed territory.",
                            NamedTextColor.RED));
        }
    }

    /**
     * Prevents players from damaging entities in claimed chunks they do not have
     * permission for. Distinguishes between passive animals and hostile monsters.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null) return;

        Entity victim = event.getEntity();
        if (victim instanceof Player) return;

        Chunk chunk = victim.getLocation().getChunk();
        ClanPermission required = isPassive(victim)
                ? ClanPermission.KILL_ANIMALS
                : ClanPermission.KILL_MONSTERS;

        if (isDenied(attacker, chunk, required)) {
            event.setCancelled(true);
            attacker.sendMessage(
                    Component.text("You cannot attack entities in claimed territory.",
                            NamedTextColor.RED));
        }
    }

    /**
     * Prevents players from breaking hanging entities (item frames, paintings) in
     * claimed chunks without {@link ClanPermission#BREAK_BLOCKS}.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Player player = resolvePlayer(event.getRemover());
        if (player == null) return;

        Chunk chunk = event.getEntity().getLocation().getChunk();
        if (isDenied(player, chunk, ClanPermission.BREAK_BLOCKS)) {
            event.setCancelled(true);
            player.sendMessage(
                    Component.text("You cannot remove entities in claimed territory.",
                            NamedTextColor.RED));
        }
    }

    /**
     * Prevents unauthorised players from interacting with blocks in claimed chunks.
     * Distinguishes between containers, doors/gates/trapdoors, and general interactions.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Chunk chunk = event.getClickedBlock().getChunk();
        Material type = event.getClickedBlock().getType();

        ClanPermission required = resolveInteractPermission(type);
        if (required == null) return;

        if (isDenied(event.getPlayer(), chunk, required)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    Component.text("You cannot interact with that in claimed territory.",
                            NamedTextColor.RED));
        }
    }

    /**
     * Removes claimed blocks from the explosion block list, preserving the visual effect.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Chunk chunk = block.getChunk();
            return territoryService.getClaimingClan(
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ()).isPresent();
        });
    }

    /**
     * Prevents block explosions (e.g. TNT) from damaging blocks in claimed chunks.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Chunk chunk = block.getChunk();
            return territoryService.getClaimingClan(
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ()).isPresent();
        });
    }

    /**
     * Returns {@code true} if the player should be denied the given action in the chunk.
     * Returns {@code false} if the chunk is unclaimed (no restriction applies).
     *
     * @param player     the acting player
     * @param chunk      the chunk in which the action is occurring
     * @param permission the permission the player needs to proceed
     * @return {@code true} if the action should be cancelled
     */
    private boolean isDenied(Player player, Chunk chunk, ClanPermission permission) {
        String world = chunk.getWorld().getName();
        Optional<UUID> ownerOpt = territoryService.getClaimingClan(world, chunk.getX(), chunk.getZ());
        if (ownerOpt.isEmpty()) return false;

        UUID ownerClanId = ownerOpt.get();
        Clan playerClan = clanCache.getOrNull(player.getUniqueId());

        // Player's clan owns this chunk: check their personal permission.
        if (playerClan != null && playerClan.clanId().equals(ownerClanId)) {
            return !playerClan.hasPermission(player.getUniqueId(), permission);
        }

        // Player is not in the owning clan, always denied.
        return true;
    }

    /**
     * Attempts to resolve the {@link Player} responsible for an entity event.
     * Returns {@code null} if the damager is not a player (e.g. a projectile with
     * no shooter, or a non-player entity).
     *
     * @param entity the damager entity from the event
     * @return the responsible player, or {@code null}
     */
    private Player resolvePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    /**
     * Returns {@code true} if the entity is a passive (non-hostile) creature.
     *
     * @param entity the entity to classify
     * @return {@code true} for animals, {@code false} for monsters and other entities
     */
    private boolean isPassive(Entity entity) {
        return entity instanceof Animals || entity instanceof WaterMob || entity instanceof Ambient;
    }

    private static final Set<Material> CONTAINERS = Set.of(
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.HOPPER,
            Material.DROPPER, Material.DISPENSER, Material.FURNACE, Material.BLAST_FURNACE,
            Material.SMOKER, Material.BREWING_STAND, Material.ANVIL, Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL);

    private static final Set<Material> INTERACTABLES = Set.of(
            Material.LEVER, Material.CRAFTING_TABLE, Material.ENCHANTING_TABLE, Material.BEACON,
            Material.LOOM, Material.CARTOGRAPHY_TABLE, Material.GRINDSTONE, Material.STONECUTTER,
            Material.FLETCHING_TABLE, Material.SMITHING_TABLE);

    /**
     * Maps an interacted block type to the permission required to interact with it.
     * Returns {@code null} if the block type requires no protection.
     *
     * @param type the material of the clicked block
     * @return the required permission, or {@code null} if unprotected
     */
    private ClanPermission resolveInteractPermission(Material type) {
        if (Tag.SHULKER_BOXES.isTagged(type) || CONTAINERS.contains(type)) return ClanPermission.OPEN_CONTAINERS;
        if (Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) || Tag.FENCE_GATES.isTagged(type)) return ClanPermission.USE_DOORS;
        if (Tag.BUTTONS.isTagged(type) || INTERACTABLES.contains(type)
                || type == Material.COMPARATOR || type == Material.REPEATER) return ClanPermission.INTERACT;
        return null;
    }
}
