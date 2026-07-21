package net.sylphian.minecraft.gathering.config;

import net.sylphian.minecraft.gathering.node.LootEntry;
import net.sylphian.minecraft.gathering.node.LootTable;
import net.sylphian.minecraft.gathering.node.NodeInteraction;
import net.sylphian.minecraft.gathering.node.NodeModifier;
import net.sylphian.minecraft.gathering.node.NodeType;
import net.sylphian.minecraft.gathering.node.ToolRequirement;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Parses a gathering module's {@code nodes} config section into engine {@link NodeType}s.
 * Node ids are prefixed with the owning module's {@code namespace}. Malformed nodes are
 * skipped and logged; a bad node never aborts the load.
 */
public final class NodeTypeConfigLoader {

    private final String namespace;
    private final NodeInteraction defaultInteraction;
    private final Logger logger;

    /**
     * @param namespace          the owning module's registry namespace, e.g. {@code "sylphian-mining"}
     * @param defaultInteraction the interaction used when a node omits {@code interaction}
     * @param logger             the owning plugin's logger
     */
    public NodeTypeConfigLoader(String namespace, NodeInteraction defaultInteraction, Logger logger) {
        this.namespace = namespace;
        this.defaultInteraction = defaultInteraction;
        this.logger = logger;
    }

    /**
     * Parses every node under the given section.
     *
     * @param nodes the {@code nodes} section, may be null
     * @return the parsed node types
     */
    public List<NodeType> load(@Nullable ConfigurationSection nodes) {
        List<NodeType> types = new ArrayList<>();
        if (nodes == null) {
            logger.warning("No 'nodes' section in config.yml; no " + namespace + " nodes will be registered.");
            return types;
        }

        for (String key : nodes.getKeys(false)) {
            ConfigurationSection node = nodes.getConfigurationSection(key);
            if (node == null) continue;

            NodeType type = parseNode(key, node);
            if (type != null) types.add(type);
        }

        logger.info("[" + namespace + "] node loading complete [" + types.size() + "] node type(s) registered.");
        return types;
    }

    private @Nullable NodeType parseNode(String key, ConfigurationSection node) {
        String id = namespace + ":" + key;

        NodeInteraction interaction = parseEnum(NodeInteraction.class, node.getString("interaction"), defaultInteraction, key, "interaction");

        Material availableBlock = Material.matchMaterial(node.getString("block", ""));
        Material depletedBlock = Material.matchMaterial(node.getString("depleted-block", ""));
        if (availableBlock == null || depletedBlock == null) {
            logger.warning("Node '" + key + "' has an unknown 'block' or 'depleted-block'; skipping.");
            return null;
        }

        ToolRequirement tool = parseTool(key, node);

        int respawnSeconds = node.getInt("respawn-seconds", 0);
        String skillId = node.getString("skill");
        long xp = node.getLong("xp", 0);
        int minSkillLevel = node.getInt("min-skill-level", 0);

        LootTable loot = new LootTable(parseLoot(node.getMapList("loot"), key));
        List<NodeModifier> modifiers = parseModifiers(node.getMapList("modifiers"), key);

        return new NodeType(id, interaction, availableBlock, depletedBlock, tool,
                respawnSeconds, skillId, xp, minSkillLevel, loot, modifiers);
    }

    private @Nullable ToolRequirement parseTool(String key, ConfigurationSection node) {
        if (!node.contains("tool")) return null;
        ToolRequirement.ToolCategory category = parseEnum(ToolRequirement.ToolCategory.class,
                node.getString("tool"), ToolRequirement.ToolCategory.ANY, key, "tool");
        ToolRequirement.ToolTier minTier = parseEnum(ToolRequirement.ToolTier.class,
                node.getString("min-tool-tier"), ToolRequirement.ToolTier.WOOD, key, "min-tool-tier");
        return new ToolRequirement(category, minTier);
    }

    private List<LootEntry> parseLoot(List<Map<?, ?>> raw, String key) {
        List<LootEntry> entries = new ArrayList<>();
        for (Map<?, ?> row : raw) {
            LootEntry entry = parseLootEntry(row, key);
            if (entry != null) entries.add(entry);
        }
        return entries;
    }

    private @Nullable LootEntry parseLootEntry(Map<?, ?> row, String key) {
        Object item = row.get("item");
        if (!(item instanceof String itemId) || itemId.indexOf(':') == -1) {
            logger.warning("Node '" + key + "' has a loot entry with a missing or malformed 'item' (expected 'namespace:id'); skipping.");
            return null;
        }
        int weight = intOr(row.get("weight"), 1);
        int minAmount = Math.max(0, intOr(row.get("min-amount"), 1));
        int maxAmount = Math.max(minAmount, intOr(row.get("max-amount"), minAmount));
        return new LootEntry(itemId, weight, minAmount, maxAmount);
    }

    private List<NodeModifier> parseModifiers(List<Map<?, ?>> raw, String key) {
        List<NodeModifier> modifiers = new ArrayList<>();
        for (Map<?, ?> row : raw) {
            String id = row.get("id") instanceof String s ? s : "modifier";
            double chance = doubleOr(row.get("chance"), 0.0);
            double yieldMultiplier = doubleOr(row.get("yield-multiplier"), 1.0);

            Material blockOverride = null;
            if (row.get("block-override") instanceof String name) {
                blockOverride = Material.matchMaterial(name);
                if (blockOverride == null) {
                    logger.warning("Node '" + key + "' modifier '" + id + "' has unknown block-override '" + name + "'; ignoring it.");
                }
            }

            List<LootEntry> bonusLoot = new ArrayList<>();
            if (row.get("bonus-loot") instanceof List<?> list) {
                for (Object element : list) {
                    if (element instanceof Map<?, ?> map) {
                        LootEntry entry = parseLootEntry(map, key);
                        if (entry != null) bonusLoot.add(entry);
                    }
                }
            }

            modifiers.add(new NodeModifier(id, chance, yieldMultiplier, bonusLoot, blockOverride));
        }
        return modifiers;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, @Nullable String value, E fallback, String key, String field) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("Node '" + key + "' has unknown " + field + " '" + value + "'; using " + fallback.name() + ".");
            return fallback;
        }
    }

    private static int intOr(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static double doubleOr(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }
}
