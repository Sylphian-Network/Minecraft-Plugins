package net.sylphian.minecraft.clans.db.models;

import java.util.UUID;

/**
 * Raw database row from the {@code sylphian_clans} table.
 *
 * @param clanId    the clan's UUID
 * @param name      the clan's display name
 * @param motd      the clan's message of the day, or null if none is set
 * @param createdAt epoch-second timestamp of when the clan was created
 */
public record ClanModel(
        UUID clanId,
        String name,
        String motd,
        long createdAt
) {}
