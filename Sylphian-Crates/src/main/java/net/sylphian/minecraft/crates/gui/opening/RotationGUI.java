package net.sylphian.minecraft.crates.gui.opening;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.crates.config.CrateConfig;
import net.sylphian.minecraft.crates.config.RewardEntry;
import net.sylphian.minecraft.crates.service.CrateService;
import net.sylphian.minecraft.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Slot-machine opening style. Rolls are predetermined; the animation is purely visual.
 *
 * <p>Layout (3-row chest):</p>
 * <ul>
 *   <li>Row 0 — decorative, center slot shows spin counter.</li>
 *   <li>Row 1 — the reel (9 scrolling item slots, center slot = landing slot).</li>
 *   <li>Row 2 — won rewards, populated left-to-right as each spin completes.</li>
 * </ul>
 *
 * <p>Each spin generates a full item tape pre-seeded so the winner lands in the
 * center reel slot on the final animation frame. After all spins complete all
 * rewards are given and the GUI closes.</p>
 */
public class RotationGUI {

    /** Total scroll steps per spin. */
    private static final int TOTAL_STEPS = 40;

    /** GUI slot indices for the reel row. */
    private static final int[] REEL_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    /** Index within REEL_SLOTS that is the landing slot (center). */
    private static final int CENTER_REEL_INDEX = 4;

    /** GUI slot for the spin counter display. */
    private static final int COUNTER_SLOT = 4;

    private static final Random RANDOM = new Random();

    private RotationGUI() {}

    /**
     * Opens the rotation GUI and begins the first spin animation.
     *
     * @param player        the player opening the crate
     * @param crate         the crate configuration
     * @param rolledRewards all pre-rolled rewards for the session
     * @param crateService  used to build reward display items
     * @param plugin        the owning plugin for scheduling
     */
    public static void open(Player player, CrateConfig crate, List<RewardEntry> rolledRewards, CrateService crateService, JavaPlugin plugin) {
        RotationGUIHolder holder = new RotationGUIHolder(crate, rolledRewards);
        Component title = Component.text("Opening: ", NamedTextColor.DARK_GRAY)
                .append(MiniMessage.miniMessage().deserialize(crate.displayName()));
        Inventory inv = Bukkit.createInventory(holder, 36, title);
        holder.setInventory(inv);

        fillDecorative(inv, rolledRewards.size());
        updateCounter(inv, 1, rolledRewards.size());

        player.openInventory(inv);
        startSpin(player, holder, crateService, plugin);
    }

    private static void startSpin(Player player, RotationGUIHolder holder, CrateService crateService, JavaPlugin plugin) {
        RewardEntry winner = holder.getRolledRewards().get(holder.getCurrentSpin());
        ItemStack winnerItem = crateService.buildItem(winner);

        int tapeSize = TOTAL_STEPS + REEL_SLOTS.length;
        int winnerPosition = (TOTAL_STEPS - 1) + CENTER_REEL_INDEX;

        List<ItemStack> tape = new ArrayList<>(tapeSize);
        for (int i = 0; i < tapeSize; i++) {
            RewardEntry random = holder.getCrate().pool().get(RANDOM.nextInt(holder.getCrate().pool().size()));
            tape.add(crateService.buildItem(random));
        }
        tape.set(winnerPosition, winnerItem);

        showReelFrame(holder.getInventory(), tape, 0);
        scheduleStep(player, holder, tape, 0, crateService, plugin);
    }

    private static void scheduleStep(Player player, RotationGUIHolder holder,
                                     List<ItemStack> tape, int step, CrateService crateService,
                                     JavaPlugin plugin) {
        long delay = delayForStep(step);
        var task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof RotationGUIHolder h) || h != holder) return;

            int nextStep = step + 1;
            showReelFrame(holder.getInventory(), tape, nextStep);

            if (nextStep >= TOTAL_STEPS - 1) {
                scheduleSpinComplete(player, holder, crateService, plugin);
            } else {
                scheduleStep(player, holder, tape, nextStep, crateService, plugin);
            }
        }, delay);

        holder.setAnimationTask(task);
    }

    private static void scheduleSpinComplete(Player player, RotationGUIHolder holder, CrateService crateService, JavaPlugin plugin) {
        var task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof RotationGUIHolder h) || h != holder) return;

            holder.incrementSpin();
            updateWonRow(holder.getInventory(), holder, crateService);

            if (holder.hasMoreSpins()) {
                updateCounter(holder.getInventory(), holder.getCurrentSpin() + 1, holder.getRolledRewards().size());
                startSpin(player, holder, crateService, plugin);
            } else {
                scheduleClose(player, holder, crateService, plugin);
            }
        }, 25L);
        holder.setAnimationTask(task);
    }

    private static void scheduleClose(Player player, RotationGUIHolder holder,
                                      CrateService crateService, JavaPlugin plugin) {
        var task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            holder.setAllSpinsComplete(true);
            holder.getRolledRewards().forEach(r -> crateService.giveReward(player, r));
            player.closeInventory();
        }, 20L);
        holder.setAnimationTask(task);
    }

    private static void showReelFrame(Inventory inv, List<ItemStack> tape, int step) {
        for (int i = 0; i < REEL_SLOTS.length; i++) {
            inv.setItem(REEL_SLOTS[i], tape.get(step + i));
        }
    }

    private static void fillDecorative(Inventory inv, int totalSpins) {
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        ItemStack winSlot = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("<dark_gray>Win Slot")
                .build();

        for (int i = 0; i < 9; i++) {
            if (i != COUNTER_SLOT) inv.setItem(i, filler);
        }
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, filler);
        }

        int startOffset = (9 - Math.min(totalSpins, 9)) / 2;
        for (int i = 0; i < 9; i++) {
            boolean isWinSlot = i >= startOffset && i < startOffset + totalSpins;
            inv.setItem(27 + i, isWinSlot ? winSlot : filler);
        }
    }

    private static void updateCounter(Inventory inv, int current, int total) {
        inv.setItem(COUNTER_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .name("<gold>Spin " + current + " <dark_gray>/ <gray>" + total)
                .build());
    }

    private static void updateWonRow(Inventory inv, RotationGUIHolder holder, CrateService crateService) {
        int totalSpins = holder.getRolledRewards().size();
        int startOffset = (9 - Math.min(totalSpins, 9)) / 2;
        int wonIndex = holder.getCurrentSpin() - 1;
        if (wonIndex < 0 || wonIndex > 8) return;
        RewardEntry won = holder.getRolledRewards().get(wonIndex);
        inv.setItem(27 + startOffset + wonIndex, crateService.buildItem(won));
    }

    /**
     * Returns the delay in ticks between animation frames.
     * Lower step = faster scroll; deceleration as step increases.
     */
    private static long delayForStep(int step) {
        if (step < 20) return 1L;
        if (step < 30) return 2L;
        if (step < 35) return 4L;
        if (step < 38) return 6L;
        return 8L;
    }
}