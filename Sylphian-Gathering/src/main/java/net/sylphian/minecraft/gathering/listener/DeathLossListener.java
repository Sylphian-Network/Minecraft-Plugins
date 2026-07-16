package net.sylphian.minecraft.gathering.listener;

import net.sylphian.minecraft.dimensions.model.Dimension;
import net.sylphian.minecraft.gathering.bridge.DimensionsBridge;
import net.sylphian.minecraft.gathering.harvest.HarvestService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Enforces a dimension's {@code death-loss-chance} against gathered items only,
 * removing that fraction of the gathered haul (rounded to the nearest item),
 * with the lost items chosen uniformly at random across every gathered stack.
 */
public final class DeathLossListener implements Listener {

    private final Random random = new Random();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Dimension dimension = DimensionsBridge.getDimensionByWorld(player.getWorld()).orElse(null);
        if (dimension == null) return;

        double chance = dimension.ruleset().deathLossChance();
        if (chance <= 0.0) return;

        if (event.getKeepInventory()) {
            removeFromInventory(player, chance);
        } else {
            removeFromDrops(event.getDrops(), chance);
        }
    }

    // Kept inventory: remove the lost fraction from the live inventory in place.
    private void removeFromInventory(Player player, double chance) {
        ItemStack[] contents = player.getInventory().getContents();

        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isGathered(contents[slot])) slots.add(slot);
        }
        if (slots.isEmpty()) return;

        int[] amounts = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) amounts[i] = contents[slots.get(i)].getAmount();

        int[] removed = pickRemovals(amounts, chance);
        for (int i = 0; i < slots.size(); i++) {
            if (removed[i] == 0) continue;
            int slot = slots.get(i);
            int newAmount = amounts[i] - removed[i];
            if (newAmount <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                contents[slot].setAmount(newAmount);
                player.getInventory().setItem(slot, contents[slot]);
            }
        }
    }

    // Dropped inventory: remove the lost fraction from the drop list before it spawns.
    private void removeFromDrops(List<ItemStack> drops, double chance) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < drops.size(); i++) {
            if (isGathered(drops.get(i))) indices.add(i);
        }
        if (indices.isEmpty()) return;

        int[] amounts = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) amounts[i] = drops.get(indices.get(i)).getAmount();

        int[] removed = pickRemovals(amounts, chance);
        List<Integer> toDelete = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            if (removed[i] == 0) continue;
            int newAmount = amounts[i] - removed[i];
            if (newAmount <= 0) {
                toDelete.add(indices.get(i));
            } else {
                drops.get(indices.get(i)).setAmount(newAmount);
            }
        }

        // Remove emptied stacks by descending index so earlier indices stay valid.
        toDelete.sort(Collections.reverseOrder());
        for (int index : toDelete) drops.remove(index);
    }

    /**
     * Chooses how many units to remove from each stack: {@code round(total * chance)}
     * units total, each drawn uniformly at random from the pooled gathered items.
     *
     * @param amounts the per-stack unit counts
     * @param chance  the fraction of the total haul to remove
     * @return the per-stack units to remove, parallel to {@code amounts}
     */
    private int[] pickRemovals(int[] amounts, double chance) {
        int total = 0;
        for (int amount : amounts) total += amount;

        int toRemove = Math.min(total, (int) Math.round(total * chance));
        int[] removed = new int[amounts.length];
        if (toRemove <= 0) return removed;

        int[] remaining = amounts.clone();
        int remainingTotal = total;
        for (int n = 0; n < toRemove; n++) {
            int roll = random.nextInt(remainingTotal);
            for (int i = 0; i < remaining.length; i++) {
                if (roll < remaining[i]) {
                    removed[i]++;
                    remaining[i]--;
                    remainingTotal--;
                    break;
                }
                roll -= remaining[i];
            }
        }
        return removed;
    }

    private static boolean isGathered(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().getPersistentDataContainer().has(HarvestService.GATHERED_KEY, PersistentDataType.STRING);
    }
}
