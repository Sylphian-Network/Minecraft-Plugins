package net.sylphian.minecraft.fishing.services;

import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.config.MutationConfig;
import net.sylphian.minecraft.fishing.services.mutation.FishContext;
import net.sylphian.minecraft.fishing.services.mutation.FishMutation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Service for managing and applying fish mutations.
 * Handles the registration of mutations and determines which ones to apply
 * based on configuration and catch context.
 */
public class FishMutationService {

    private ConfigLoader config;
    private final Map<String, FishMutation> mutations = new LinkedHashMap<>();
    private final Random random = new Random();

    /**
     * Constructs a new FishMutationService.
     *
     * @param config the configuration loader
     */
    public FishMutationService(ConfigLoader config) {
        this.config = config;
    }

    /**
     * Registers a new mutation with the service.
     *
     * @param id       the mutation identifier
     * @param mutation the mutation implementation
     */
    public void registerMutation(String id, FishMutation mutation) {
        mutations.put(id, mutation);
    }

    /**
     * Attempts to apply all registered and enabled mutations to a caught fish.
     * Each mutation's chance is modified by the fish's rarity multiplier and
     * any active bait zone mutation multipliers from the catch context.
     *
     * @param player  the player who caught the fish
     * @param item    the fish item stack
     * @param context the catch context
     */
    public void applyMutations(Player player, ItemStack item, FishContext context) {
        if (item == null || item.getType().isAir()) return;

        for (Map.Entry<String, FishMutation> entry : mutations.entrySet()) {
            String id = entry.getKey();
            FishMutation mutation = entry.getValue();

            MutationConfig mutConfig = config.getMutationConfig(id);

            if (!mutConfig.enabled()) continue;

            double multiplier = context.getRarity() != null
                    ? context.getRarity().getMutationMultiplier()
                    : 1.0;
            double finalChance = mutConfig.baseChance() * multiplier * context.getMutationChanceMultiplier();

            if (random.nextDouble() < finalChance && mutation.shouldApply(player, context)) {
                mutation.apply(item, player, context);
            }
        }
    }

    /**
     * Retrieves the configuration for a given mutation ID.
     *
     * @param id the mutation identifier
     * @return the MutationConfig, or an empty disabled config if not found
     */
    public MutationConfig getMutationConfig(String id) {
        return config.getMutationConfig(id);
    }

    /**
     * Reloads the service with updated configuration.
     * Registered mutations are preserved — only config values are refreshed.
     *
     * @param config the new configuration loader
     */
    public void reload(ConfigLoader config) {
        this.config = config;
    }
}
