package net.sylphian.minecraft.gathering;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.gathering.command.SylphianGatheringCommand;
import net.sylphian.minecraft.gathering.config.GatheringConfig;
import net.sylphian.minecraft.gathering.harvest.HarvestService;
import net.sylphian.minecraft.gathering.listener.DeathLossListener;
import net.sylphian.minecraft.gathering.listener.GatheringListener;
import net.sylphian.minecraft.gathering.registry.GatheringNodeRegistry;
import net.sylphian.minecraft.gathering.registry.GatheringNodeService;
import net.sylphian.minecraft.gathering.world.NodeManager;
import net.sylphian.minecraft.gathering.world.RespawnScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class SylphianGathering extends JavaPlugin {

    private GatheringConfig config;
    private NodeManager nodeManager;
    private RespawnScheduler respawnScheduler;
    private HarvestService harvestService;

    private boolean resolveScheduled;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        config = GatheringConfig.from(getConfig(), getLogger());

        nodeManager = new NodeManager(this, config);
        respawnScheduler = new RespawnScheduler(this, nodeManager);
        harvestService = new HarvestService(nodeManager, respawnScheduler, config, getLogger());

        GatheringNodeService.init(nodeManager);

        getServer().getPluginManager().registerEvents(nodeManager, this);
        getServer().getPluginManager().registerEvents(new GatheringListener(nodeManager, harvestService), this);
        getServer().getPluginManager().registerEvents(new DeathLossListener(), this);

        GatheringNodeRegistry.setChangeListener(this::requestResolve);
        requestResolve();

        new SylphianGatheringCommand(this, nodeManager).register();

        getLogger().info("Sylphian Gathering enabled!");
    }

    @Override
    public void onDisable() {
        GatheringNodeRegistry.setChangeListener(null);
        GatheringNodeService.shutdown();
        if (respawnScheduler != null) respawnScheduler.clear();
        if (nodeManager != null) nodeManager.clearAll();
        getLogger().info("Sylphian Gathering disabled.");
    }

    private void requestResolve() {
        if (resolveScheduled) return;
        resolveScheduled = true;
        Bukkit.getScheduler().runTask(this, () -> {
            resolveScheduled = false;
            nodeManager.resolve();
        });
    }

    /**
     * Re-reads config and re-resolves placements without restarting the plugin.
     * A bad edit keeps the running state and reports the failure.
     *
     * @param sender the sender to notify of the outcome
     */
    public void reload(CommandSender sender) {
        try {
            reloadConfig();
            config = GatheringConfig.from(getConfig(), getLogger());
            harvestService.reload(config);
            nodeManager.reload(config);
            respawnScheduler.clear();
            nodeManager.resolve();
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Sylphian-Gathering reloaded."));
        } catch (Exception e) {
            getLogger().severe("Reload failed, keeping previous state: " + e.getMessage());
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Reload failed: " + e.getMessage()));
        }
    }
}
