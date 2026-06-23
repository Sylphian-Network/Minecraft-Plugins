package net.sylphian.minecraft.clans.db.models;

import java.util.UUID;

/**
 * Represents a single named clan warp.
 *
 * @param clanId      the owning clan's UUID
 * @param name        the warp name, unique within the clan
 * @param world       the world name
 * @param x           block-precise X coordinate
 * @param y           block-precise Y coordinate
 * @param z           block-precise Z coordinate
 * @param yaw         player yaw at the time the warp was set
 * @param pitch       player pitch at the time the warp was set
 * @param icon        the Material name used as the warp's GUI icon
 * @param description a short description shown in the warp GUI
 * @param restricted  whether use is limited to the access list; if false, any clan member may use it
 */
public record ClanWarpModel(
        UUID clanId,
        String name,
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String icon,
        String description,
        boolean restricted
) {}
