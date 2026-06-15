package net.sylphian.minecraft.clans.db.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Raw database row from the {@code clan_claims} table.
 *
 * @param world      the name of the world containing the claimed chunk
 * @param chunkX     the chunk's X coordinate
 * @param chunkZ     the chunk's Z coordinate
 * @param clanId     the clan that owns this chunk
 * @param claimedAt  when the chunk was claimed
 */
public record ClaimModel(
        String world,
        int chunkX,
        int chunkZ,
        UUID clanId,
        Instant claimedAt
) {}
