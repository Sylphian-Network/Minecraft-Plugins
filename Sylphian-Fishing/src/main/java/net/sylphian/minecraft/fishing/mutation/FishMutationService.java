package net.sylphian.minecraft.fishing.mutation;

import net.sylphian.minecraft.fishing.config.ConfigLoader;
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

    private final ConfigLoader config;
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
     * Each mutation has a chance to occur, which is modified by the fish's rarity.
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

            // Skip if the mutation is disabled in the config
            if (!config.isMutationEnabled(id)) continue;

            double baseChance = config.getMutationBaseChance(id);
            // Apply rarity-based multiplier to the base mutation chance
            double multiplier = context.getRarity() != null ? context.getRarity().getMutationMultiplier() : 1.0;
            double finalChance = baseChance * multiplier;

            // Roll for mutation success
            if (random.nextDouble() < finalChance) {
                if (mutation.shouldApply(player, context)) {
                    mutation.apply(item, player, context);
                }
            }
        }
    }
}
