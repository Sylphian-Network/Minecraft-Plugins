package net.sylphian.minecraft.skills.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.items.util.ItemBuilder;
import net.sylphian.minecraft.skills.config.SkillsConfig;
import net.sylphian.minecraft.skills.service.SkillsService;
import net.sylphian.minecraft.skills.skill.Ability;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.Skill;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The per-skill ability detail GUI.
 *
 * <p>Layout (6 rows, 54 slots): ability items fill slots 0+ left-to-right. The
 * bottom row (slots 45-53) is a black glass pane border. Slot 48 holds the back
 * button; slot 49 holds the skill summary item.</p>
 *
 * <p>The inventory holder is a {@link SkillDetailHolder} that stores the parent
 * {@link SkillsMenu} for back navigation and the skill being displayed.</p>
 */
public final class SkillDetailMenu {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static final int BOTTOM_ROW_START = 45;
    private static final int INFO_SLOT        = 49;

    private final SkillsService service;
    private final SkillsMenu parent;

    /**
     * @param service the skills service for level and XP lookups
     * @param parent  the parent menu, used when the player navigates back
     */
    public SkillDetailMenu(SkillsService service, SkillsMenu parent) {
        this.service = service;
        this.parent  = parent;
    }

    /**
     * Opens the ability detail view for the given skill and player.
     *
     * @param player the player to show the GUI to
     * @param skill  the skill whose abilities are displayed
     */
    public void open(Player player, Skill skill) {
        Component title = MINI.deserialize("<gold><bold>" + skill.getDisplayName());
        Inventory inv = Bukkit.createInventory(
                new SkillDetailHolder(parent, skill), 54, title);

        UUID uuid = player.getUniqueId();
        SkillsConfig config = service.getConfig();
        int  level = service.getCachedLevel(uuid, skill.getId());
        long xp    = service.getCachedXP(uuid, skill.getId());

        List<Ability> abilities = skill.getAbilities();
        for (int i = 0; i < abilities.size() && i < BOTTOM_ROW_START; i++) {
            inv.setItem(i, abilityItem(abilities.get(i), level));
        }

        ItemStack border = border();
        for (int i = BOTTOM_ROW_START; i < 54; i++) {
            inv.setItem(i, border);
        }

        inv.setItem(SkillDetailHolder.BACK_SLOT, backButton());
        inv.setItem(INFO_SLOT, infoItem(skill, level, xp, config));

        player.openInventory(inv);
    }

    private static ItemStack abilityItem(Ability ability, int playerLevel) {
        boolean unlocked = playerLevel >= ability.unlockLevel();

        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + ability.description());
        lore.add("");
        if (ability instanceof PassiveAbility passive) {
            lore.add("<dark_gray>Triggers: <gray>" + passive.triggerCondition());
        }
        if (ability instanceof ActiveAbility active) {
            lore.add("<dark_gray>Activation: <gray>" + active.activation());
        }
        lore.add("");

        if (unlocked) {
            lore.add("<green>Unlocked at level " + ability.unlockLevel());
        } else {
            lore.add("<red>Unlocks at level " + ability.unlockLevel());
        }

        ItemBuilder builder = new ItemBuilder(unlocked ? Material.LIME_DYE : Material.GRAY_DYE)
                .name((unlocked ? "<green>" : "<gray>") + ability.name())
                .loreStrings(lore);

        if (unlocked) builder.glint();

        return builder.build();
    }

    private static ItemStack infoItem(Skill skill, int level, long xp, SkillsConfig config) {
        int cap = config.levelCap();
        boolean atCap = level >= cap;

        List<String> lore = new ArrayList<>();
        lore.add("<white>Level: <gold>" + level + " <dark_gray>/ <gray>" + cap);
        lore.add("");

        if (atCap) {
            lore.add("<gold>MAX LEVEL");
        } else {
            long xpAtLevel  = config.xpForLevel(level);
            long xpAtNext   = config.xpForLevel(level + 1);
            long gained     = xp - xpAtLevel;
            long needed     = xpAtNext - xpAtLevel;
            double fraction = needed > 0 ? (double) gained / needed : 1.0;

            lore.add(SkillsMenu.progressBar(fraction, 20));
            lore.add("<dark_gray>XP to next level: <gray>" + SkillsMenu.formatNumber(config.xpToNextLevel(xp)));
        }

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("<gold><bold>" + skill.getDisplayName())
                .loreStrings(lore)
                .build();
    }

    private static ItemStack backButton() {
        return new ItemBuilder(Material.ARROW)
                .name("<gray>← Back")
                .lore("<dark_gray>Return to the skills overview.")
                .build();
    }

    private static ItemStack border() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("<dark_gray> ")
                .build();
    }
}
