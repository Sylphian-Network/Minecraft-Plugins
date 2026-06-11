package net.sylphian.minecraft.crates.config;

import org.bukkit.Material;

import java.util.List;

/**
 * Immutable definition of a crate and its reward pool.
 *
 * <p>The {@link OpeningStyle} controls how rewards are presented when the crate is
 * opened. {@code totalRolls} and {@code playerPicks} are interpreted differently
 * per style — see {@link OpeningStyle} for details.</p>
 *
 * @param id              unique identifier matching the key in crates.yml
 * @param displayName     MiniMessage formatted name shown in the GUI
 * @param displayMaterial material used to represent the crate in the GUI
 * @param totalRolls      how many rewards are rolled from the pool on opening
 * @param playerPicks     how many rewards or pane clicks the player is given
 * @param openingStyle    how the opening sequence is presented to the player
 * @param pool            the weighted reward pool to roll from
 */
public record CrateConfig(
        String id,
        String displayName,
        Material displayMaterial,
        int totalRolls,
        int playerPicks,
        OpeningStyle openingStyle,
        List<RewardEntry> pool
) {}