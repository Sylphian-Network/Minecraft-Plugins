package net.sylphian.minecraft.clans.db.models;

import java.util.UUID;

/**
 * Raw database row from the {@code clans} table.
 *
 * @param clanId    the clan's UUID
 * @param name      the clan's display name
 * @param tag       the clan's short tag
 * @param createdAt epoch-second timestamp of when the clan was created
 */
public record ClanModel(
        UUID clanId,
        String name,
        String tag,
        long createdAt
) {}
