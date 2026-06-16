package net.sylphian.minecraft.clans.db.models;

import java.util.UUID;

/**
 * Represents a clan's stored home location.
 *
 * @param clanId the owning clan's UUID
 * @param world  the world name
 * @param x      block-precise X coordinate
 * @param y      block-precise Y coordinate
 * @param z      block-precise Z coordinate
 * @param yaw    player yaw at the time the home was set
 * @param pitch  player pitch at the time the home was set
 */
public record ClanHomeModel(UUID clanId, String world, double x, double y, double z, float yaw, float pitch) {}
