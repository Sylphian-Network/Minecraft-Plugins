package net.sylphian.minecraft.clans.db.models;

import java.util.UUID;

/**
 * Raw database row from the {@code sylphian_clan_members} table.
 *
 * @param playerUuid the member's Mojang UUID
 * @param clanId     the clan this member belongs to
 * @param isLeader   {@code true} if this member is the clan leader
 * @param joinedAt   epoch-second timestamp of when the player joined the clan
 */
public record ClanMemberModel(
        UUID playerUuid,
        UUID clanId,
        boolean isLeader,
        long joinedAt
) {}
