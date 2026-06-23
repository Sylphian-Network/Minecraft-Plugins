package net.sylphian.minecraft.fishing.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.skill.FishingSkillConfig;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.Ability;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Active perk unlocked at level 15.
 *
 * <p>When activated, the player's next catch yields a second copy of the item,
 * delivered one tick after the original to ensure proper inventory handling.</p>
 */
public final class DoubleHaul implements Ability {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Cooldown key used with {@link CooldownManager}. */
    public static final String COOLDOWN_ID = "fishing:double-haul";

    @Override public String id()           { return COOLDOWN_ID; }
    @Override public String name()         { return "Double Haul"; }
    @Override public String description()  { return "Your next catch yields a second copy of the item."; }
    @Override public int    unlockLevel()  { return 15; }

    /**
     * Marks the next catch for duplication and starts the cooldown.
     *
     * @param player  the activating player
     * @param uuid    the player's UUID
     * @param cfg     current config snapshot
     * @param cd      cooldown manager
     * @param pending the set tracking whose next catch is duplicated
     */
    public void activate(Player player, UUID uuid, FishingSkillConfig cfg,
                         CooldownManager cd, Set<UUID> pending) {
        pending.add(uuid);
        cd.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.doubleHaulCooldownSeconds()));
        player.sendActionBar(MINI.deserialize(
                "<aqua>Double Haul <white>ready! Your next catch will be duplicated."));
    }

    /**
     * Gives the player a second copy of the caught item if this catch is pending.
     * The clone is delivered one tick later so the original item lands in inventory first.
     *
     * @param player  the catching player
     * @param uuid    the player's UUID
     * @param caught  the item that was caught
     * @param plugin  the owning plugin (used for scheduling)
     * @param pending the set tracking whose next catch is duplicated
     */
    public void applyOnCatch(Player player, UUID uuid, ItemStack caught,
                             Plugin plugin, Set<UUID> pending) {
        if (!pending.remove(uuid)) return;
        ItemStack clone = caught.clone();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.getInventory().addItem(clone);
                player.sendActionBar(MINI.deserialize(
                        "<aqua>Double Haul: <white>Second catch added to your inventory!"));
            }
        }, 2L);
    }
}
