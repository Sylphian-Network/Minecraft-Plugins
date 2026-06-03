package net.sylphian.minecraft.crates.config;

import org.bukkit.Material;

import java.util.List;

/**
 * Immutable definition of a crate and its reward pool.
 *
 * <p>When opened, the crate rolls {@code totalRolls} rewards from the pool
 * using weighted random selection. The player then receives or picks
 * {@code playerPicks} of those rolled rewards.</p>
 *
 * <p>If {@code playerPicks} is equal to or greater than {@code totalRolls},
 * all rolled rewards are given directly. Otherwise a selection GUI is shown.</p>
 *
 * @param id              unique identifier matching the key in crates.yml
 * @param displayName     MiniMessage formatted name shown in the GUI
 * @param displayMaterial material used to represent the crate in the GUI
 * @param totalRolls      how many rewards are rolled from the pool on opening
 * @param playerPicks     how many of the rolled rewards the player may select;
 *                        players may confirm with fewer than this number
 * @param pool            the weighted reward pool to roll from
 */
public record CrateConfig(
        String id,
        String displayName,
        Material displayMaterial,
        int totalRolls,
        int playerPicks,
        List<RewardEntry> pool
) {}