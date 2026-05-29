package net.sylphian.minecraft.fishing.db.models;

import java.util.UUID;

public record FishEncyclopaediaModel(
        UUID uuid,
        String fishId,
        int timesCaught,
        double biggestWeight,
        long firstCaught,
        long lastCaught
) {}