package net.sylphian.minecraft.fishing.mutation;

import net.sylphian.minecraft.fishing.config.ConfigLoader;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FishMutationService {

    private final ConfigLoader config;
    private final Map<String, FishMutation> mutations = new HashMap<>();
    private final Random random = new Random();

    public FishMutationService(ConfigLoader config) {
        this.config = config;
    }

    public void registerMutation(String id, FishMutation mutation) {
        mutations.put(id, mutation);
    }

    public void applyMutations(Player player, ItemStack item, FishContext context) {
        if (item == null || item.getType().isAir()) return;

        for (Map.Entry<String, FishMutation> entry : mutations.entrySet()) {
            String id = entry.getKey();
            FishMutation mutation = entry.getValue();

            if (!config.isMutationEnabled(id)) continue;

            double baseChance = config.getMutationBaseChance(id);
            double multiplier = context.getRarity() != null ? context.getRarity().getMutationMultiplier() : 1.0;
            double finalChance = baseChance * multiplier;

            if (random.nextDouble() < finalChance) {
                if (mutation.shouldApply(player, context)) {
                    mutation.apply(item, player, context);
                }
            }
        }
    }
}
