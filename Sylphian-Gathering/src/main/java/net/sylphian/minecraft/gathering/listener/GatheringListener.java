package net.sylphian.minecraft.gathering.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.harvest.HarvestService;
import net.sylphian.minecraft.gathering.node.NodeInteraction;
import net.sylphian.minecraft.gathering.node.NodeType;
import net.sylphian.minecraft.gathering.node.ToolRequirement;
import net.sylphian.minecraft.gathering.world.LiveNode;
import net.sylphian.minecraft.gathering.world.NodeManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Intercepts breaking and right-clicking on live nodes and routes them to the
 * harvest service. Runs at LOW so a cancelled node event no-ops the dimension's
 * NORMAL building-rule handler via {@code ignoreCancelled}.
 */
public final class GatheringListener implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final NodeManager nodeManager;
    private final HarvestService harvestService;

    public GatheringListener(NodeManager nodeManager, HarvestService harvestService) {
        this.nodeManager = nodeManager;
        this.harvestService = harvestService;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        LiveNode node = nodeManager.lookup(event.getBlock());
        if (node == null || node.state() != LiveNode.State.AVAILABLE || node.type().interaction() != NodeInteraction.BREAK) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (toolCheckFails(player, node.type())) return;
        harvestService.harvest(player, node);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        LiveNode node = nodeManager.lookup(block);
        if (node == null || node.state() != LiveNode.State.AVAILABLE || node.type().interaction() != NodeInteraction.INTERACT) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (toolCheckFails(player, node.type())) return;
        harvestService.harvest(player, node);
    }

    private boolean toolCheckFails(Player player, NodeType type) {
        ToolRequirement tool = type.tool();
        if (tool == null) return false;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (tool.matches(held)) return false;

        player.sendMessage(MINI.deserialize("<red>You need a <white>"
                + tool.minTier().name().toLowerCase(Locale.ROOT) + "</white>-tier <white>"
                + tool.category().name().toLowerCase(Locale.ROOT) + "</white> or better to gather this."));
        return true;
    }
}
