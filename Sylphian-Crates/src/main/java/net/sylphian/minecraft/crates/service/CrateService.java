package net.sylphian.minecraft.crates.service;

import net.sylphian.minecraft.crates.config.CrateConfig;
import net.sylphian.minecraft.crates.config.RewardEntry;
import net.sylphian.minecraft.crates.config.RewardType;
import net.sylphian.minecraft.core.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service responsible for rolling and granting crate rewards.
 *
 * <p>On opening, the service rolls {@link CrateConfig#totalRolls()} rewards
 * from the crate's weighted pool. If {@link CrateConfig#playerPicks()} is
 * less than {@code totalRolls}, the caller is responsible for presenting the
 * rolled rewards to the player for selection. If {@code playerPicks} is equal
 * to or greater than {@code totalRolls}, all rolled rewards are granted immediately.</p>
 */
public class CrateService {

    private final Random random = new Random();

    /**
     * Constructs a new CrateService.
     */
    public CrateService() {
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
     * For {@link RewardType#ITEM} rewards, the item is added to the player's inventory
     * or dropped at their feet if full.
     *
     * @param player the player to reward
     * @param reward the reward to grant
     */
    public void giveReward(Player player, RewardEntry reward) {
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
     * Builds the full ItemStack for a reward, including name, lore, amount and enchantments.
     *
     * @param reward the reward to build an item for
     * @return the built ItemStack
     */
    public ItemStack buildItem(RewardEntry reward) {
        ItemBuilder builder = new ItemBuilder(reward.displayMaterial())
                .name(reward.displayName())
                .amount(reward.amount());
        if (!reward.lore().isEmpty()) builder.loreStrings(reward.lore());
        reward.enchantments().forEach(builder::enchant);
        return builder.build();
    }
}