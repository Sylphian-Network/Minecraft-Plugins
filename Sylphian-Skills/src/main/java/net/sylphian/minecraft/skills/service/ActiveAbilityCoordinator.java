package net.sylphian.minecraft.skills.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.skills.gui.AbilitySelectionHolder;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.Skill;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Generic coordinator for active-ability selection and activation.
 *
 * <p>Any skill that returns a non-empty {@link Skill#activationMaterial()} participates
 * automatically. When a player sneaks and right-clicks while holding the trigger material,
 * the coordinator opens a small inventory GUI showing their unlocked active abilities.
 * Clicking an ability item closes the menu and calls {@link ActiveAbility#onActivate}.</p>
 *
 * <p>If {@link Skill#canInteract} returns {@code false} (e.g. a hook is already in the
 * water), the interact event is left uncancelled so the game handles it naturally
 * (e.g. reel-in).</p>
 */
public final class ActiveAbilityCoordinator implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SkillsService service;
    private final Plugin plugin;
    private final Map<UUID, BukkitTask> refreshTasks = new HashMap<>();

    /**
     * @param service the skills service, used to enumerate skills and look up player levels
     * @param plugin the plugin instance, used to schedule tasks
     */
    public ActiveAbilityCoordinator(SkillsService service, Plugin plugin) {
        this.service = service;
        this.plugin = plugin;
    }

    /**
     * Intercepts sneak + right-click with a skill's trigger material.
     * Opens the ability selection GUI for the matching skill.
     * If {@link Skill#canInteract} returns {@code false}, the event is left uncancelled
     * so the game handles it normally (e.g. reel-in).
     *
     * <p>Does not use {@code ignoreCancelled}: Paper pre-cancels {@code RIGHT_CLICK_AIR}
     * events for tools like fishing rods before custom handlers run, so we must handle
     * the event regardless and apply our own cancellation.</p>
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> { /* proceed */ }
            default -> { return; }
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        Material held = player.getInventory().getItemInMainHand().getType();
        Block clickedBlock = event.getClickedBlock();
        UUID uuid = player.getUniqueId();

        for (Skill skill : service.getSkills()) {
            boolean materialMatch = skill.activationMaterial().map(m -> m == held).orElse(false);
            boolean blockMatch = event.getAction() == Action.RIGHT_CLICK_BLOCK
                    && clickedBlock != null
                    && skill.isActivationTarget(clickedBlock);
            if (!materialMatch && !blockMatch) continue;

            if (!skill.canInteract(player, uuid)) return;

            int level = service.getCachedLevel(uuid, skill.getId());
            List<ActiveAbility> actives = unlockedActives(skill, level);
            if (actives.isEmpty()) return;

            event.setCancelled(true);
            openAbilityMenu(player, uuid, actives, blockMatch ? clickedBlock : null);
            return;
        }
    }

    /**
     * Handles clicks inside an ability selection menu.
     * Cancels all clicks to prevent item movement, then activates the clicked ability.
     */
    @EventHandler(ignoreCancelled = true)
    public void onAbilityMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AbilitySelectionHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.getUniqueId().equals(holder.getOwnerUuid())) return;

        ActiveAbility ability = holder.getAbilityAt(event.getRawSlot());
        if (ability == null) return;

        player.closeInventory();
        ability.onActivate(player, player.getUniqueId(), holder.getTargetBlock());
    }

    /** Cancels all drag events inside an ability selection menu. */
    @EventHandler(ignoreCancelled = true)
    public void onAbilityMenuDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AbilitySelectionHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAbilityMenuClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof AbilitySelectionHolder holder)) return;
        BukkitTask task = refreshTasks.remove(holder.getOwnerUuid());
        if (task != null) task.cancel();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BukkitTask task = refreshTasks.remove(event.getPlayer().getUniqueId());
        if (task != null) task.cancel();
    }

    private void openAbilityMenu(Player player, UUID uuid, List<ActiveAbility> actives, Block targetBlock) {
        AbilitySelectionHolder holder = new AbilitySelectionHolder(actives, uuid, targetBlock);
        Inventory inv = Bukkit.createInventory(holder, 9,
                MINI.deserialize("<dark_aqua>Select Ability"));

        for (Map.Entry<Integer, ActiveAbility> entry : holder.getSlotMap().entrySet()) {
            inv.setItem(entry.getKey(), abilityItem(entry.getValue(), uuid));
        }

        player.openInventory(inv);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<Integer, ActiveAbility> entry : holder.getSlotMap().entrySet()) {
                inv.setItem(entry.getKey(), abilityItem(entry.getValue(), uuid));
            }
        }, 20L, 20L);
        refreshTasks.put(uuid, task);
    }

    private ItemStack abilityItem(ActiveAbility ability, UUID uuid) {
        String status = ability.selectionStatus(uuid);
        ItemStack item = ItemStack.of(statusMaterial(ability.statusLevel(uuid)));
        item.editMeta(meta -> {
            meta.displayName(MINI.deserialize("<!italic><gold>" + ability.name()));
            meta.lore(List.of(
                    MINI.deserialize("<!italic><gray>" + ability.description()),
                    Component.empty(),
                    MINI.deserialize("<!italic>" + status),
                    Component.empty(),
                    MINI.deserialize("<!italic><dark_gray>Click to activate")
            ));
        });
        return item;
    }

    /**
     * Picks a material that visually reflects the ability's current state.
     */
    private static Material statusMaterial(StatusLevel level) {
        return switch (level) {
            case READY       -> Material.LIME_DYE;
            case PENDING     -> Material.YELLOW_DYE;
            case ACTIVE      -> Material.GOLD_NUGGET;
            case ON_COOLDOWN -> Material.GRAY_DYE;
        };
    }

    private static List<ActiveAbility> unlockedActives(Skill skill, int level) {
        return skill.getAbilities().stream()
                .filter(a -> a instanceof ActiveAbility && level >= a.unlockLevel())
                .map(a -> (ActiveAbility) a)
                .toList();
    }
}
