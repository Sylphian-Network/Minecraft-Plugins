package net.sylphian.minecraft.gathering.harvest;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.bridge.ItemsBridge;
import net.sylphian.minecraft.gathering.bridge.SkillsBridge;
import net.sylphian.minecraft.gathering.config.GatheringConfig;
import net.sylphian.minecraft.gathering.node.LootEntry;
import net.sylphian.minecraft.gathering.node.NodeModifier;
import net.sylphian.minecraft.gathering.node.NodeType;
import net.sylphian.minecraft.gathering.world.LiveNode;
import net.sylphian.minecraft.gathering.world.NodeManager;
import net.sylphian.minecraft.gathering.world.RespawnScheduler;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Runs the harvest of one live node: skill gate, weighted loot roll with the
 * active modifier applied, item delivery, XP award, and depletion.
 */
public final class HarvestService {

    /** Stamped on every gathered stack with the node id, so death loss can find them. */
    public static final NamespacedKey GATHERED_KEY = new NamespacedKey("sylphian-gathering", "gathered");

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final NodeManager nodeManager;
    private final RespawnScheduler respawnScheduler;
    private final Logger logger;
    private final Random random = new Random();

    private GatheringConfig config;

    public HarvestService(NodeManager nodeManager, RespawnScheduler respawnScheduler, GatheringConfig config, Logger logger) {
        this.nodeManager = nodeManager;
        this.respawnScheduler = respawnScheduler;
        this.config = config;
        this.logger = logger;
    }

    /**
     * Swaps in a reloaded config so respawn defaults stay current.
     *
     * @param newConfig the reloaded config
     */
    public void reload(GatheringConfig newConfig) {
        this.config = newConfig;
    }

    /**
     * Harvests the node for the player: rolls loot, awards XP, and depletes the
     * block. Does nothing but message the player if a skill-level gate fails.
     *
     * @param player the harvesting player
     * @param node   the available node being harvested
     * @return true if the node was harvested and depleted
     */
    public boolean harvest(Player player, LiveNode node) {
        NodeType type = node.type();
        UUID uuid = player.getUniqueId();

        if (type.skillId() != null && type.minSkillLevel() > 0
                && SkillsBridge.level(uuid, type.skillId()) < type.minSkillLevel()) {
            player.sendMessage(MINI.deserialize("<red>You need <white>" + type.skillId() + "</white> level <white>" + type.minSkillLevel() + "</white> to gather this."));
            return false;
        }

        NodeModifier modifier = node.activeModifier();
        double yieldMultiplier = modifier != null ? modifier.yieldMultiplier() : 1.0;
        Location dropAt = new Location(node.world(), node.x() + 0.5, node.y() + 0.5, node.z() + 0.5);

        LootEntry base = type.loot().roll(random);
        if (base != null) {
            int amount = (int) Math.round(base.rollAmount(random) * yieldMultiplier);
            giveLoot(player, dropAt, base.itemId(), amount, type.id());
        }

        if (modifier != null) {
            for (LootEntry bonus : modifier.bonusLoot()) {
                giveLoot(player, dropAt, bonus.itemId(), bonus.rollAmount(random), type.id());
            }
        }

        SkillsBridge.awardXp(player, type.skillId(), type.xp());

        int respawnSeconds = type.respawnSeconds() > 0 ? type.respawnSeconds() : config.defaultRespawnSeconds();
        node.setState(LiveNode.State.DEPLETED);
        node.setRespawnDeadline(System.currentTimeMillis() + respawnSeconds * 1000L);
        nodeManager.applyBlockState(node);
        respawnScheduler.schedule(node);
        return true;
    }

    private void giveLoot(Player player, Location dropAt, String itemId, int amount, String nodeId) {
        if (amount <= 0) return;

        ItemStack template = ItemsBridge.resolve(itemId).orElse(null);
        if (template == null) {
            logger.warning("Node '" + nodeId + "' loot references unknown item '" + itemId + "'; skipping that drop.");
            return;
        }

        int remaining = amount;
        int maxStack = template.getMaxStackSize();
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack stack = template.clone();
            stack.setAmount(stackAmount);
            stack.editMeta(meta -> meta.getPersistentDataContainer()
                    .set(GATHERED_KEY, PersistentDataType.STRING, nodeId));

            for (ItemStack overflow : player.getInventory().addItem(stack).values()) {
                player.getWorld().dropItemNaturally(dropAt, overflow);
            }
            remaining -= stackAmount;
        }
    }
}
