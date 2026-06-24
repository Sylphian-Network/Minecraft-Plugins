package net.sylphian.minecraft.skills;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.database.DatabaseService;
import net.sylphian.minecraft.skills.api.SkillsProvider;
import net.sylphian.minecraft.skills.command.SkillsAdminCommand;
import net.sylphian.minecraft.skills.command.SkillsPlayerCommand;
import net.sylphian.minecraft.skills.gui.SkillsMenu;
import net.sylphian.minecraft.skills.config.SkillsConfig;
import net.sylphian.minecraft.skills.listener.SkillsMenuListener;
import net.sylphian.minecraft.skills.db.migrations.Migration001CreatePlayerSkills;
import net.sylphian.minecraft.skills.db.repositories.SkillRepository;
import net.sylphian.minecraft.skills.listener.SkillsListener;
import net.sylphian.minecraft.skills.placeholder.SkillsPlaceholderExpansion;
import net.sylphian.minecraft.skills.service.SkillsService;
import net.sylphian.minecraft.skills.skill.SkillRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main plugin class for Sylphian-Skills.
 *
 * <p>Wires together the framework layer: XP persistence, level-up effects,
 * placeholders, and admin commands. Individual skills are contributed by their
 * owning plugins via {@link net.sylphian.minecraft.skills.api.SkillsAPI#registerSkill}.</p>
 */
public final class SylphianSkills extends JavaPlugin {

    private SkillsService skillsService;
    private SkillRegistry skillRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        DatabaseService.registerMigrations(List.of(new Migration001CreatePlayerSkills()));
        DatabaseService.runMigrations("Sylphian-Skills", getLogger());

        SkillsConfig config = SkillsConfig.from(getConfig(), getLogger());

        SkillRepository repository = new SkillRepository(DatabaseService.getJdbi(), DatabaseService.getExecutor());

        skillRegistry = new SkillRegistry();
        skillsService = new SkillsService(repository, skillRegistry, this, config);

        SkillsProvider.register(skillsService);

        SkillsMenu skillsMenu = new SkillsMenu(skillsService);

        new SkillsAdminCommand(this).register();
        new SkillsPlayerCommand(skillsMenu).register();

        getServer().getPluginManager().registerEvents(new SkillsListener(skillsService), this);
        getServer().getPluginManager().registerEvents(new SkillsMenuListener(), this);

        Bukkit.getOnlinePlayers().forEach(p -> skillsService.load(p.getUniqueId()));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SkillsPlaceholderExpansion(skillsService).register();
        }

        getLogger().info("Sylphian-Skills initialised.");
    }

    /**
     * Re-reads {@code config.yml} and applies new settings to the live service.
     * On failure the old config is kept and the error is reported.
     *
     * @param sender the command sender to notify, or {@code null} for a silent reload
     */
    public void reload(CommandSender sender) {
        try {
            reloadConfig();
            SkillsConfig fresh = SkillsConfig.from(getConfig(), getLogger());
            skillsService.reload(fresh);
            getLogger().info("Configuration reloaded.");
            if (sender != null) {
                sender.sendMessage(Component.text("Skills config reloaded.", NamedTextColor.GREEN));
            }
        } catch (Exception e) {
            getLogger().severe("Reload failed, keeping old config: " + e.getMessage());
            if (sender != null) {
                sender.sendMessage(Component.text("Reload failed: see console.", NamedTextColor.RED));
            }
        }
    }

    @Override
    public void onDisable() {
        SkillsProvider.unregister();
        getLogger().info("Sylphian-Skills disabled.");
    }
}
