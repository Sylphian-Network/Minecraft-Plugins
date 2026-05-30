package net.sylphian.minecraft.fishing.db.models;

import java.util.UUID;

/**
 * Data model representing a player's entry in the fish encyclopaedia.
 *
 * @param uuid          the player's unique identifier
 * @param fishId        the unique ID of the fish
 * @param timesCaught   how many times the player has caught this fish
 * @param biggestWeight the largest recorded weight for this fish catch
 * @param firstCaught   epoch timestamp of the first catch
 * @param lastCaught    epoch timestamp of the most recent catch
 */
public record FishEncyclopaediaModel(
        UUID uuid,
        String fishId,
        int timesCaught,
        double biggestWeight,
        long firstCaught,
        long lastCaught
) {}