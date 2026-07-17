package net.sylphian.minecraft.mining;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.node.NodeType;
import net.sylphian.minecraft.gathering.registry.GatheringNodeRegistry;
import net.sylphian.minecraft.items.item.ItemRegistry;
import net.sylphian.minecraft.mining.command.SylphianMiningCommand;
import net.sylphian.minecraft.mining.config.NodeTypeConfigLoader;
import net.sylphian.minecraft.mining.item.MiningItemProvider;
import net.sylphian.minecraft.mining.skill.SkillsBridge;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SylphianMining extends JavaPlugin {

    private static final String NAMESPACE = "sylphian-mining";

    private MiningItemProvider itemProvider;
    private @Nullable SkillsBridge skillsBridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        List<NodeType> nodeTypes = new NodeTypeConfigLoader(getLogger()).load(getConfig().getConfigurationSection("nodes"));
        GatheringNodeRegistry.register(nodeTypes);

        itemProvider = new MiningItemProvider(getConfig().getConfigurationSection("items"), getLogger());
        ItemRegistry.register(itemProvider);

        if (getServer().getPluginManager().getPlugin("Sylphian-Skills") != null) {
            skillsBridge = new SkillsBridge(this);
        }

        new SylphianMiningCommand(this).register();

        getLogger().info("Sylphian Mining enabled! [" + nodeTypes.size() + " node type(s) loaded]");
    }

    @Override
    public void onDisable() {
        GatheringNodeRegistry.unregister(NAMESPACE);
        ItemRegistry.unregister(NAMESPACE);
        if (skillsBridge != null) skillsBridge.unregister();
        getLogger().info("Sylphian Mining disabled.");
    }

    /**
     * Rebuilds and re-registers node types and item definitions from disk. The
     * re-registration signals Sylphian-Gathering to re-resolve live nodes.
     *
     * @param sender the sender to notify of the outcome
     */
    public void reload(CommandSender sender) {
        try {
            reloadConfig();
            GatheringNodeRegistry.unregister(NAMESPACE);
            List<NodeType> nodeTypes = new NodeTypeConfigLoader(getLogger()).load(getConfig().getConfigurationSection("nodes"));
            GatheringNodeRegistry.register(nodeTypes);
            itemProvider.reload(getConfig().getConfigurationSection("items"));
            if (skillsBridge != null) skillsBridge.reload();
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Sylphian-Mining reloaded [" + nodeTypes.size() + " node type(s)]."));
        } catch (Exception e) {
            getLogger().severe("Reload failed, keeping previous state: " + e.getMessage());
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Reload failed: " + e.getMessage()));
        }
    }
}
