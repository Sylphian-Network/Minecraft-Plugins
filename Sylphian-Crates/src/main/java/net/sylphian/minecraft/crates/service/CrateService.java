package net.sylphian.minecraft.crates.service;

import net.sylphian.minecraft.items.item.ItemRegistry;
import net.sylphian.minecraft.crates.config.CrateConfig;
import net.sylphian.minecraft.crates.config.RewardEntry;
import net.sylphian.minecraft.crates.config.RewardType;
import net.sylphian.minecraft.crates.economy.CrateEconomy;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Service responsible for rolling and granting crate rewards.
 *
 * <p>Provides weighted random rolling via {@link #rollRewards} and {@link #rollOne},
 * item building, and reward granting. The caller is responsible for determining
 * how rolled rewards are presented based on the crate's {@link net.sylphian.minecraft.crates.config.OpeningStyle}.</p>
 */
public class CrateService {

    private final Random random = new Random();

    /**
     * Constructs a new CrateService.
     */
    public CrateService() {
    }

    /**
     * Rolls a single reward from the crate's weighted pool.
     *
     * @param crate the crate to roll from
     * @return the rolled RewardEntry
     */
    public RewardEntry rollOne(CrateConfig crate) {
        double totalWeight = crate.pool().stream().mapToDouble(RewardEntry::chance).sum();
        double roll = random.nextDouble() * totalWeight;
        double cursor = 0;
        for (RewardEntry entry : crate.pool()) {
            cursor += entry.chance();
            if (roll <= cursor) return entry;
        }
        return crate.pool().get(crate.pool().size() - 1);
    }

    /**
     * Rolls rewards from the crate's pool according to {@link CrateConfig#totalRolls()}.
     * Uses weighted random selection — entries with higher chances appear more frequently.
     *
     * @param crate the crate configuration to roll from
     * @return the list of rolled RewardEntry objects
     */
    public List<RewardEntry> rollRewards(CrateConfig crate) {
        List<RewardEntry> rolled = new ArrayList<>();
        double totalWeight = crate.pool().stream()
                .mapToDouble(RewardEntry::chance)
                .sum();

        for (int i = 0; i < crate.totalRolls(); i++) {
            double roll = random.nextDouble() * totalWeight;
            double cursor = 0;

            for (RewardEntry entry : crate.pool()) {
                cursor += entry.chance();
                if (roll <= cursor) {
                    rolled.add(entry);
                    break;
                }
            }
        }

        return rolled;
    }

    /**
     * Grants a single reward to the player.
     * The item is added to the player's inventory, or dropped at their feet if full.
     *
     * @param player the player to reward
     * @param reward the reward to grant
     */
    public void giveReward(Player player, RewardEntry reward) {
        if (reward.type() == RewardType.MONEY) {
            if (Bukkit.getPluginManager().getPlugin("Sylphian-Economy") != null) {
                CrateEconomy.deposit(player.getUniqueId(), reward.money());
            }
            return;
        }
        ItemStack item = buildItem(reward);
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    /**
     * Grants all rewards in the provided list to the player directly.
     * Intended for cases where {@link CrateConfig#playerPicks()} equals or
     * exceeds {@link CrateConfig#totalRolls()}.
     *
     * @param player  the player to reward
     * @param rewards the rewards to grant
     */
    public void giveAll(Player player, List<RewardEntry> rewards) {
        rewards.forEach(reward -> giveReward(player, reward));
    }

    /**
     * Builds the ItemStack for a reward.
     * For external item rewards, the item is resolved from the {@link ItemRegistry}
     * and cloned with the configured amount applied. Falls back to a named paper
     * placeholder if the registry lookup fails.
     * For standard rewards, the item is built from the display material, name,
     * lore, and enchantments.
     *
     * @param reward the reward to build an item for
     * @return the built ItemStack
     */
    public ItemStack buildItem(RewardEntry reward) {
        if (reward.externalItemId() != null) {
            Optional<ItemStack> registryItem = ItemRegistry.get(reward.externalItemId());
            if (registryItem.isPresent()) {
                ItemStack copy = registryItem.get().clone();
                copy.setAmount(reward.amount());
                return copy;
            }
            return new ItemBuilder(Material.PAPER)
                    .name("<red>Missing: " + reward.externalItemId())
                    .build();
        }
        ItemBuilder builder = new ItemBuilder(reward.displayMaterial())
                .name(reward.displayName())
                .amount(reward.amount());
        if (!reward.lore().isEmpty()) builder.loreStrings(reward.lore());
        reward.enchantments().forEach(builder::enchant);
        return builder.build();
    }
}