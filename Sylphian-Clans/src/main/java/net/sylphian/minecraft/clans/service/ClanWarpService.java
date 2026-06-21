package net.sylphian.minecraft.clans.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.clans.db.api.IClanWarpRepository;
import net.sylphian.minecraft.clans.db.models.ClanWarpModel;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Business logic for clan warps and their per-warp access.
 *
 * <p>A warp is either public (any clan member may use it) or restricted (only the
 * leader, holders of {@link ClanPermission#MANAGE_WARP}, and explicitly granted
 * members may use it). Warps are not cached; reads hit the database directly.</p>
 */
public final class ClanWarpService {

    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_DESCRIPTION_LENGTH = 256;
    private static final MiniMessage DESCRIPTION_MINI = MiniMessage.miniMessage();

    private final IClanWarpRepository warpRepository;
    private volatile int maxWarpsPerClan;

    /**
     * @param warpRepository  persistence layer for warps and access
     * @param maxWarpsPerClan the maximum number of warps a single clan may own
     */
    public ClanWarpService(IClanWarpRepository warpRepository, int maxWarpsPerClan) {
        this.warpRepository = warpRepository;
        this.maxWarpsPerClan = maxWarpsPerClan;
    }

    /**
     * Updates the maximum warps-per-clan limit. Called after a config reload.
     *
     * @param max the new limit
     */
    public void setMaxWarpsPerClan(int max) {
        this.maxWarpsPerClan = max;
    }

    /**
     * @return the maximum number of warps a single clan may own
     */
    public int getMaxWarpsPerClan() {
        return maxWarpsPerClan;
    }

    /**
     * Creates a new warp, or updates the location, icon, and description of an existing one.
     * A new warp is public and counts against the per-clan limit; updating an existing warp
     * does not change its restriction state and is not subject to the limit.
     *
     * @param clanId      the owning clan
     * @param name        the warp name (1-32 chars: letters, digits, hyphen, underscore)
     * @param location    the destination; its world must be loaded
     * @param iconName    the Material name for the GUI icon
     * @param description a short description (max 256 chars)
     * @return a future that completes when the warp is persisted
     * @throws IllegalArgumentException if the name, icon, or description is invalid
     * @throws IllegalStateException    if creating a new warp would exceed the per-clan limit
     */
    public CompletableFuture<Void> saveWarp(UUID clanId, String name, Location location, String iconName, String description) {
        validateName(name);
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Warp description must be at most " + MAX_DESCRIPTION_LENGTH + " characters."));
        }

        ClanWarpModel model = new ClanWarpModel(
                clanId,
                name,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                iconName,
                description == null ? "" : sanitizeDescription(description),
                false
        );

        return warpRepository.getWarp(clanId, name).thenCompose(existing -> {
            if (existing.isPresent()) {
                return warpRepository.saveWarp(model);
            }
            return warpRepository.countWarps(clanId).thenCompose(count -> {
                if (count >= maxWarpsPerClan) {
                    return CompletableFuture.failedFuture(
                            new IllegalStateException("Your clan has reached the maximum of " + maxWarpsPerClan + " warps."));
                }
                return warpRepository.saveWarp(model);
            });
        });
    }

    /**
     * Removes a warp and its access list.
     *
     * @param clanId the owning clan
     * @param name   the warp name
     * @return a future that is {@code true} if a warp was removed, {@code false} if none existed
     */
    public CompletableFuture<Boolean> removeWarp(UUID clanId, String name) {
        return warpRepository.getWarp(clanId, name).thenCompose(existing -> {
            if (existing.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            return warpRepository.deleteWarp(clanId, name).thenApply(v -> true);
        });
    }

    /**
     * @param clanId the owning clan
     * @param name   the warp name
     * @return a future of the warp, or empty if none exists
     */
    public CompletableFuture<Optional<ClanWarpModel>> getWarp(UUID clanId, String name) {
        return warpRepository.getWarp(clanId, name);
    }

    /**
     * @param clanId the owning clan
     * @return a future of all the clan's warps, ordered by name
     */
    public CompletableFuture<List<ClanWarpModel>> listWarps(UUID clanId) {
        return warpRepository.listWarps(clanId);
    }

    /**
     * Sets whether a warp is restricted to its access list.
     *
     * @param clanId     the owning clan
     * @param name       the warp name
     * @param restricted the new restriction state
     * @return a future that completes when the change is persisted
     */
    public CompletableFuture<Void> setRestricted(UUID clanId, String name, boolean restricted) {
        return warpRepository.setRestricted(clanId, name, restricted);
    }

    /**
     * Grants a member access to a warp.
     *
     * @param clanId   the owning clan
     * @param name     the warp name
     * @param playerId the member to grant
     * @return a future that completes when access is granted
     */
    public CompletableFuture<Void> grantAccess(UUID clanId, String name, UUID playerId) {
        return warpRepository.grantAccess(clanId, name, playerId);
    }

    /**
     * Revokes a member's access to a warp.
     *
     * @param clanId   the owning clan
     * @param name     the warp name
     * @param playerId the member to revoke
     * @return a future that completes when access is revoked
     */
    public CompletableFuture<Void> revokeAccess(UUID clanId, String name, UUID playerId) {
        return warpRepository.revokeAccess(clanId, name, playerId);
    }

    /**
     * @param clanId the owning clan
     * @param name   the warp name
     * @return a future of the UUIDs of all members explicitly granted access
     */
    public CompletableFuture<List<UUID>> listAccess(UUID clanId, String name) {
        return warpRepository.listAccess(clanId, name);
    }

    /**
     * Determines whether a member may teleport to a warp. Public warps are usable by any
     * member; restricted warps require the leader, {@link ClanPermission#MANAGE_WARP}, or an
     * explicit access grant.
     *
     * @param clan   the member's clan
     * @param player the member to test
     * @param warp   the warp to test
     * @return a future that is {@code true} if the member may use the warp
     */
    public CompletableFuture<Boolean> canUse(Clan clan, UUID player, ClanWarpModel warp) {
        if (!warp.restricted() || clan.hasPermission(player, ClanPermission.MANAGE_WARP)) {
            return CompletableFuture.completedFuture(true);
        }
        return warpRepository.hasAccess(clan.clanId(), warp.name(), player);
    }

    /**
     * Returns the names of every warp the member has an explicit access grant for. Used to
     * resolve usability for a whole warp list in a single query.
     *
     * @param clanId   the owning clan
     * @param playerId the member to look up
     * @return a future of the set of warp names the member can access
     */
    public CompletableFuture<Set<String>> accessibleWarps(UUID clanId, UUID playerId) {
        return warpRepository.listAccessibleWarps(clanId, playerId).thenApply(HashSet::new);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH
                || !name.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Warp name must be 1-" + MAX_NAME_LENGTH + " characters: letters, digits, hyphen, or underscore.");
        }
    }

    // Parses MiniMessage input and removes click/hover/insertion so a stored description can never carry events.
    private String sanitizeDescription(String raw) {
        return DESCRIPTION_MINI.serialize(stripInteractivity(DESCRIPTION_MINI.deserialize(raw)));
    }

    private Component stripInteractivity(Component component) {
        Component out = component
                .clickEvent(null)
                .hoverEvent(null)
                .insertion(null);
        return out.children(out.children().stream().map(this::stripInteractivity).toList());
    }
}

