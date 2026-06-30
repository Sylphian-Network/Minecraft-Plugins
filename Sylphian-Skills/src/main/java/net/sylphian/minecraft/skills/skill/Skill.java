package net.sylphian.minecraft.skills.skill;

import net.sylphian.minecraft.skills.api.SkillsAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Contract for a single skill implementation.
 *
 * <p>Each skill is responsible for identifying itself and registering the
 * Bukkit event listeners that award XP by calling back into
 * {@link SkillsAPI#awardXP}.</p>
 */
public interface Skill {

    /**
     * @return the unique lowercase identifier for this skill, e.g. {@code "mining"}
     */
    String getId();

    /**
     * @return the player-facing display name, e.g. {@code "Mining"}
     */
    String getDisplayName();

    /**
     * Registers this skill's Bukkit event listeners with the server.
     * Called once during plugin enable after the service is ready.
     *
     * @param plugin the owning plugin (used as listener owner)
     * @param api    the skills API for XP awards, cooldowns, and buff tracking
     */
    void registerListeners(Plugin plugin, SkillsAPI api);

    /**
     * Returns the abilities unlocked as the player levels up this skill.
     * The default returns an empty list; override via {@link AbstractSkill#addAbility}.
     *
     * @return ordered list of abilities, from lowest to highest unlock level
     */
    default List<Ability> getAbilities() {
        return List.of();
    }

    /**
     * The item material a player must be holding for the framework's active-ability
     * coordinator to intercept sneak-right-click gestures.
     * Return {@link Optional#empty()} for skills with no active abilities.
     *
     * @return the trigger material, or empty if not applicable
     */
    default Optional<Material> activationMaterial() {
        return Optional.empty();
    }

    /**
     * Block types that trigger the active-ability coordinator when a player
     * sneak-right-clicks them, regardless of the item held. The clicked block is
     * passed to {@link ActiveAbility#onActivate(Player, UUID, org.bukkit.block.Block)}.
     * Return an empty set for skills that activate from a held item instead.
     *
     * @return the trigger block types, or an empty set if not applicable
     */
    default Set<Material> activationBlocks() {
        return Set.of();
    }

    /**
     * Returns {@code false} to suppress active-ability activation for this player
     * right now, without cancelling the underlying item interaction.
     * For example, a fishing skill returns false while the hook is in the water
     * so the player can still reel in normally.
     *
     * @param player the interacting player
     * @param uuid   the player's UUID
     * @return {@code true} if activation should proceed
     */
    default boolean canInteract(Player player, UUID uuid) {
        return true;
    }

    /**
     * Called when the owning plugin reloads its config. No-op by default.
     */
    default void reload() {}
}
