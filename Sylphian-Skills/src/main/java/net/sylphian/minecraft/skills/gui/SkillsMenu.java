package net.sylphian.minecraft.skills.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.items.util.ItemBuilder;
import net.sylphian.minecraft.skills.config.SkillsConfig;
import net.sylphian.minecraft.skills.service.SkillsService;
import net.sylphian.minecraft.skills.skill.Skill;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The skill category browser GUI.
 *
 * <p>Layout (6 rows, 54 slots): skill items fill rows 1-5 (slots 0-44) left-to-right,
 * top-to-bottom. The bottom row (slots 45-53) is a black glass pane border with a
 * summary item at slot 49.</p>
 *
 * <p>The inventory holder is a {@link SkillsMenuHolder} that stores the ordered skill
 * list so the listener can resolve a clicked slot back to a skill without PDC or title
 * matching.</p>
 */
public final class SkillsMenu {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Component TITLE = MINI.deserialize("<gold><bold>Skills");

    private static final int BOTTOM_ROW_START = 45;
    private static final int INFO_SLOT = 49;

    private final SkillsService service;
    private final SkillDetailMenu detailMenu;

    /**
     * @param service the skills service for level and XP lookups
     */
    public SkillsMenu(SkillsService service) {
        this.service    = service;
        this.detailMenu = new SkillDetailMenu(service, this);
    }

    /**
     * Opens the skill category list for the given player.
     *
     * @param player the player to show the GUI to
     */
    public void open(Player player) {
        Collection<Skill> skills = service.getSkills();
        List<Skill> skillList = List.copyOf(skills);

        Inventory inv = Bukkit.createInventory(
                new SkillsMenuHolder(this, skillList), 54, TITLE);

        UUID uuid = player.getUniqueId();
        SkillsConfig config = service.getConfig();

        for (int i = 0; i < skillList.size() && i < BOTTOM_ROW_START; i++) {
            Skill skill = skillList.get(i);
            int  level  = service.getCachedLevel(uuid, skill.getId());
            long xp     = service.getCachedXP(uuid, skill.getId());
            inv.setItem(i, skillItem(skill, level, xp, config));
        }

        ItemStack border = border();
        for (int i = BOTTOM_ROW_START; i < 54; i++) {
            inv.setItem(i, border);
        }
        inv.setItem(INFO_SLOT, infoItem());

        player.openInventory(inv);
    }

    /**
     * Opens the ability detail view for the given skill.
     *
     * @param player the player to show the GUI to
     * @param skill  the skill to display
     */
    public void openDetail(Player player, Skill skill) {
        detailMenu.open(player, skill);
    }

    private static ItemStack skillItem(Skill skill, int level, long xp, SkillsConfig config) {
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

            lore.add(progressBar(fraction, 20));
            lore.add("<dark_gray>XP to next level: <gray>" + formatNumber(config.xpToNextLevel(xp)));
        }

        lore.add("");
        lore.add("<gray>Click to view abilities");

        return new ItemBuilder(level > 0 ? Material.ENCHANTED_BOOK : Material.BOOK)
                .name("<gold><bold>" + skill.getDisplayName())
                .loreStrings(lore)
                .build();
    }

    private static ItemStack infoItem() {
        return new ItemBuilder(Material.NETHER_STAR)
                .name("<yellow>Skills")
                .lore("<gray>Click a category to view its abilities.")
                .build();
    }

    private static ItemStack border() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("<dark_gray> ")
                .build();
    }

    /**
     * Builds a MiniMessage progress bar string.
     *
     * @param fraction value in [0, 1]
     * @param length   total number of characters
     * @return a colored bar string, e.g. {@code "<green>████░░░░░░<dark_gray>░░░░░░"}
     */
    static String progressBar(double fraction, int length) {
        int filled = (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * length);
        return "<green>" + "█".repeat(filled) + "<dark_gray>" + "░".repeat(length - filled);
    }

    /**
     * Formats a number with thousands separators.
     *
     * @param n the number to format
     * @return formatted string, e.g. {@code "1,250"}
     */
    static String formatNumber(long n) {
        return String.format("%,d", n);
    }
}
