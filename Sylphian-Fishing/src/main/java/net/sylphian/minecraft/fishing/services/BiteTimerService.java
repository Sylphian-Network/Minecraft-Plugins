package net.sylphian.minecraft.fishing.services;

import net.sylphian.minecraft.fishing.config.BiteTimerConfig;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Service responsible for overriding the vanilla fishing timers.
 *
 * <p>On cast, calculates a custom wait time (how long until a fish appears) and lure
 * time (how long the fish nibbles before biting), both based on a pre-rolled rarity
 * estimate and — for wait time — the current weather condition. Rarer fish take longer
 * to appear and longer to commit to the bite.</p>
 *
 * <p>Vanilla lure enchantment influence, sky influence, and rain influence are disabled
 * so the calculated values are applied exactly as configured.</p>
 *
 * <p>The pre-rolled rarity is used only for timing — the actual catch rolls
 * independently when the fish bites.</p>
 */
public class BiteTimerService {

    private ConfigLoader config;
    private final LootService lootService;
    private final BaitZoneService baitZoneService;
    private final Logger logger;
    private final Random random = new Random();

    /**
     * Constructs a new BiteTimerService.
     *
     * @param config      the config loader providing bite timer settings
     * @param lootService the loot service used to peek at a likely rarity
     * @param baitZoneService the service used to check for active bait zones at the hook location
     * @param logger      the logger for debug output
     */
    public BiteTimerService(ConfigLoader config, LootService lootService, BaitZoneService baitZoneService, Logger logger) {
        this.config = config;
        this.lootService = lootService;
        this.baitZoneService = baitZoneService;
        this.logger = logger;
    }

    /**
     * Calculates and applies custom wait and lure times to the given fishing hook.
     * Disables vanilla lure enchantment, sky, and rain influences so the configured
     * values are applied exactly. Called on {@link org.bukkit.event.player.PlayerFishEvent.State#FISHING}.
     *
     * @param hook   the fishing hook to apply the timers to
     * @param player the player who cast the hook
     */
    public void applyBiteTimer(FishHook hook, Player player) {
        WeatherCondition weather = WeatherCondition.from(player.getWorld());
        Rarity preRolledRarity = lootService.peekRarity(weather);

        BiteTimerConfig timerConfig = config.getBiteTimerConfig();
        int delay = timerConfig.calculate(preRolledRarity, weather, random);
        int lureDelay = timerConfig.calculateLureTime(preRolledRarity, random);

        double biteTimerMult = baitZoneService.getBiteTimerMultiplier(hook.getLocation());
        delay = Math.max(20, (int) (delay * biteTimerMult));

        hook.setApplyLure(false);
        hook.setSkyInfluenced(false);
        hook.setRainInfluenced(false);

        hook.setWaitTime(delay, delay);
        hook.setLureTime(lureDelay, lureDelay);

        logger.fine("Bite timer set to " + delay + " ticks (bait mult: " + biteTimerMult
                + "), lure time " + lureDelay + " ticks for "
                + preRolledRarity.getId() + " rarity in " + weather.name());
    }

    /**
     * Reloads the service with updated configuration.
     *
     * @param config the new configuration loader
     */
    public void reload(ConfigLoader config) {
        this.config = config;
    }
}