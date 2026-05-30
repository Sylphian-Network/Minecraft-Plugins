package net.sylphian.minecraft.fishing.mutation;

import net.sylphian.minecraft.fishing.fish.Rarity;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * Contextual information for a fish catch.
 * Passed to mutations to determine if they should apply based on the
 * rarity of the fish, the biome, or the player who caught it.
 */
public class FishContext {
    private final Rarity rarity;
    private final Biome biome;
    private final Player player;

    /**
     * Constructs a new FishContext.
     *
     * @param rarity the rarity of the caught fish
     * @param biome  the biome where the fish was caught
     * @param player the player who caught the fish
     */
    public FishContext(Rarity rarity, Biome biome, Player player) {
        this.rarity = rarity;
        this.biome = biome;
        this.player = player;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public Biome getBiome() {
        return biome;
    }

    public Player getPlayer() {
        return player;
    }
}
