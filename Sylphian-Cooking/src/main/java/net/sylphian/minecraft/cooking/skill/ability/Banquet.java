package net.sylphian.minecraft.cooking.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.cooking.skill.CookingSkillConfig;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active ability unlocked at level 5. Applies a short haste, regeneration, and saturation buff
 * to all players near the cooking station.
 */
public final class Banquet implements ActiveAbility {

    /** Cooldown key used with {@link CooldownManager}. */
    public static final String COOLDOWN_ID = "cooking:banquet";

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<CookingSkillConfig> config;
    private final CooldownManager cooldownManager;

    /**
     * @param config          supplier for the current config snapshot
     * @param cooldownManager the shared cooldown manager
     */
    public Banquet(Supplier<CookingSkillConfig> config, CooldownManager cooldownManager) {
        this.config = config;
        this.cooldownManager = cooldownManager;
    }

    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Banquet"; }
    @Override public String description() { return "Grants nearby players haste, regeneration, and saturation."; }
    @Override public int    unlockLevel() { return 5; }

    @Override
    public void onActivate(Player player, UUID uuid) {
        onActivate(player, uuid, null);
    }

    @Override
    public void onActivate(Player player, UUID uuid, @Nullable Block target) {
        long remaining = cooldownManager.getRemainingSeconds(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Banquet: <white>" + remaining + "s remaining."));
            return;
        }

        CookingSkillConfig cfg = config.get();
        Location center = target != null ? target.getLocation().add(0.5, 0.5, 0.5) : player.getLocation();
        if (center.getWorld() == null) return;

        int durationTicks = cfg.banquetDurationSeconds() * 20;
        int amplifier = cfg.banquetAmplifier();
        int count = 0;
        for (Player nearby : center.getWorld().getNearbyPlayers(center, cfg.banquetRadius())) {
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, durationTicks, amplifier, true, true));
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, amplifier, true, true));
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, durationTicks, amplifier, true, true));
            nearby.sendActionBar(MINI.deserialize("<gold>A banquet restores you!"));
            count++;
        }

        cooldownManager.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.banquetCooldownSeconds()));
        player.sendActionBar(MINI.deserialize("<gold>Banquet! <white>Buffed " + count + " player" + (count == 1 ? "" : "s") + "."));
    }

    @Override
    public String selectionStatus(UUID uuid) {
        long s = cooldownManager.getRemainingSeconds(uuid, COOLDOWN_ID);
        return s > 0 ? "<red>" + s + "s" : "<green>Ready";
    }

    @Override
    public StatusLevel statusLevel(UUID uuid) {
        return cooldownManager.isOnCooldown(uuid, COOLDOWN_ID) ? StatusLevel.ON_COOLDOWN : StatusLevel.READY;
    }
}
