package net.sylphian.minecraft.fishing.services;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.config.RarityCatchEffects;
import net.sylphian.minecraft.fishing.fish.Rarity;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * Applies visual and audio catch effects to the player based on the rarity
 * of the fish they caught. All effect types (sound, particle, title, broadcast)
 * are driven by config.yml and can be individually enabled or disabled per rarity.
 */
public class CatchEffectService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private ConfigLoader config;
    private final Logger logger;

    /**
     * Constructs a new CatchEffectService.
     *
     * @param config the configuration loader for retrieving rarity effect settings
     */
    public CatchEffectService(ConfigLoader config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Applies all configured catch effects for the given catch.
     *
     * @param player   the player who caught the fish
     * @param rarity   the rarity of the catch
     * @param fishId   the ID of the caught entry, used in broadcast messages
     * @param location the location of the fishing hook
     */
    public void apply(Player player, Rarity rarity, String fishId, Location location) {
        RarityCatchEffects effects = config.getRarityCatchEffects(rarity);

        applySound(player, effects.sound());
        applyParticles(location, effects.particle());
        applyTitle(player, effects.title());
        applyBroadcast(player, fishId, effects.broadcast());
    }

    /**
     * Plays a sound to the catching player.
     *
     * @param player the player to play the sound to
     * @param sound  the sound config, or null if disabled
     */
    private void applySound(Player player, RarityCatchEffects.SoundConfig sound) {
        if (sound == null) return;

        NamespacedKey key = NamespacedKey.fromString(sound.name());
        Sound bukkitSound = Registry.SOUNDS.get(key);

        if (bukkitSound == null) {
            logger.warning("Invalid sound '" + sound.name() + "' in catch-effects config - skipping.");
            return;
        }

        player.playSound(player.getLocation(), bukkitSound, sound.volume(), sound.pitch());
    }

    /**
     * Spawns particles at the hook location.
     *
     * @param location the location to spawn particles at
     * @param particle the particle config, or null if disabled
     */
    private void applyParticles(Location location, RarityCatchEffects.ParticleConfig particle) {
        if (particle == null || location.getWorld() == null) return;

        try {
            Particle bukkitParticle = Particle.valueOf(particle.type().toUpperCase());
            location.getWorld().spawnParticle(
                    bukkitParticle,
                    location,
                    particle.count(),
                    particle.offsetX(),
                    particle.offsetY(),
                    particle.offsetZ()
            );
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid particle type '" + particle.type() + "' in catch-effects config - skipping.");
        }
    }

    /**
     * Shows a title to the catching player.
     *
     * @param player the player to show the title to
     * @param title  the title config, or null if disabled
     */
    private void applyTitle(Player player, RarityCatchEffects.TitleConfig title) {
        if (title == null) return;

        player.showTitle(Title.title(
                MINI.deserialize(title.title()),
                MINI.deserialize(title.subtitle()),
                Title.Times.times(
                        Duration.ofMillis(title.fadeIn() * 50L),
                        Duration.ofMillis(title.stay() * 50L),
                        Duration.ofMillis(title.fadeOut() * 50L)
                )
        ));
    }

    /**
     * Broadcasts a server-wide message for the catch.
     * Supports {player} and {fish} placeholders.
     *
     * @param player    the catching player
     * @param fishId    the ID of the caught entry
     * @param broadcast the broadcast config, or null if disabled
     */
    private void applyBroadcast(Player player, String fishId, RarityCatchEffects.BroadcastConfig broadcast) {
        if (broadcast == null) return;

        String message = broadcast.message()
                .replace("{player}", player.getName())
                .replace("{fish}", fishId);

        Bukkit.broadcast(MINI.deserialize(message));
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