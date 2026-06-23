package net.sylphian.minecraft.clans.config;

import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Immutable snapshot of reloadable settings from config.yml.
 *
 * @param maxClaimsPerClan  max chunks a clan may own
 * @param maxWarpsPerClan   max warps a clan may create
 * @param inviteExpiry      seconds before an invite expires
 * @param teleportWarmup    seconds a player must stand still before a warp teleports
 * @param defaultMemberPerms permissions granted to a new member on join
 */
public record ClansConfig(int maxClaimsPerClan, int maxWarpsPerClan,
                          long inviteExpiry, int teleportWarmup,
                          List<ClanPermission> defaultMemberPerms) {

    /** Builds a config snapshot from disk, skipping and logging unknown permission names. */
    public static ClansConfig from(FileConfiguration cfg, Logger log) {
        List<ClanPermission> perms = cfg.getStringList("default-member-permissions").stream()
                .flatMap(raw -> ClanPermission.parse(raw)
                        .or(() -> { log.warning("Unknown permission '" + raw + "', skipping."); return Optional.empty(); })
                        .stream())
                .toList();

        return new ClansConfig(
                cfg.getInt("max-claims-per-clan", 50),
                cfg.getInt("max-warps-per-clan", 5),
                cfg.getLong("invite-expiry-seconds", 300),
                cfg.getInt("home-warmup-seconds", 3),
                perms);
    }
}