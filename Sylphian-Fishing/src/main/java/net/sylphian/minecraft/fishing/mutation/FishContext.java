package net.sylphian.minecraft.fishing.mutation;

import net.sylphian.minecraft.fishing.fish.Rarity;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

public class FishContext {
    private final Rarity rarity;
    private final Biome biome;
    private final Player player;

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
