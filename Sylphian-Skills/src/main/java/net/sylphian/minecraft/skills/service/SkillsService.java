package net.sylphian.minecraft.skills.service;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.sylphian.minecraft.skills.api.SkillsAPI;
import net.sylphian.minecraft.skills.config.SkillsConfig;
import net.sylphian.minecraft.skills.db.api.ISkillRepository;
import net.sylphian.minecraft.skills.event.SkillLevelUpEvent;
import net.sylphian.minecraft.skills.event.SkillMaxLevelEvent;
import net.sylphian.minecraft.skills.event.SkillXPGainEvent;
import net.sylphian.minecraft.skills.skill.Skill;
import net.sylphian.minecraft.skills.skill.SkillRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core service implementing {@link SkillsAPI}.
 *
 * <p>Maintains an in-memory XP cache seeded on player join and cleared on quit.
 * Every XP gain is written through to the database asynchronously. Level is
 * always derived from XP at the point of use, never stored, so config changes
 * to the curve take effect on the next read.</p>
 */
public class SkillsService implements SkillsAPI {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** uuid to (skillId to totalXP). */
    private final Map<UUID, Map<String, Long>> cache = new ConcurrentHashMap<>();

    private final ISkillRepository repository;
    private final SkillRegistry registry;
    private final Plugin plugin;
    private volatile SkillsConfig config;

    private final CooldownManager cooldownManager = new CooldownManager();
    private final ActiveBuffTracker activeBuffTracker = new ActiveBuffTracker();

    /**
     * @param repository the persistence layer
     * @param registry   the registry of all registered skills
     * @param plugin     the owning plugin (for event dispatch and scheduler)
     * @param config     the initial config snapshot
     */
    public SkillsService(ISkillRepository repository, SkillRegistry registry,
                         Plugin plugin, SkillsConfig config) {
        this.repository = repository;
        this.registry = registry;
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Swaps the config reference. Safe to call from the main thread during a reload.
     *
     * @param config the new config snapshot
     */
    public void reload(SkillsConfig config) {
        this.config = config;
    }

    /**
     * @return the current config snapshot
     */
    public SkillsConfig getConfig() {
        return config;
    }

    /**
     * Loads all skill XP from the database into the cache for a joining player.
     * Non-blocking; the DB read runs async and the result lands in the thread-safe cache.
     *
     * @param uuid the joining player's UUID
     */
    public void load(UUID uuid) {
        repository.loadAll(uuid).thenAccept(data ->
                cache.put(uuid, new ConcurrentHashMap<>(data)));
    }

    /**
     * Removes a player's data from the cache when they quit.
     * Write-through on every XP gain means no flush is needed here.
     * Also clears cooldown and buff state so memory does not accumulate.
     *
     * @param uuid the quitting player's UUID
     */
    public void unload(UUID uuid) {
        cache.remove(uuid);
        cooldownManager.clearPlayer(uuid);
        activeBuffTracker.clearPlayer(uuid);
    }

    @Override
    public void registerSkill(Skill skill, Plugin owningPlugin) {
        registry.register(skill);
        skill.registerListeners(owningPlugin, this);
    }

    @Override
    public void unregisterSkill(String skillId) {
        registry.unregister(skillId);
    }

    @Override
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    @Override
    public ActiveBuffTracker getActiveBuffTracker() {
        return activeBuffTracker;
    }

    @Override
    public long getCachedXP(UUID uuid, String skillId) {
        Map<String, Long> playerSkills = cache.get(uuid);
        if (playerSkills == null) return 0L;
        return playerSkills.getOrDefault(skillId, 0L);
    }

    @Override
    public int getCachedLevel(UUID uuid, String skillId) {
        return config.levelFromXp(getCachedXP(uuid, skillId));
    }

    @Override
    public void awardXP(Player player, String skillId, long amount) {
        if (amount <= 0) return;

        Optional<Skill> skillOpt = registry.get(skillId);
        if (skillOpt.isEmpty()) return;
        Skill skill = skillOpt.get();

        UUID uuid = player.getUniqueId();
        if (isAtCap(uuid, skillId)) return;

        SkillsConfig snapshot = config;

        Map<String, Long> playerSkills = cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        long before = playerSkills.getOrDefault(skillId, 0L);
        long after  = Math.min(before + amount, snapshot.xpForLevel(snapshot.levelCap()));

        playerSkills.put(skillId, after);

        repository.upsertXP(uuid, skillId, after)
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to persist XP for " + player.getName()
                            + " skill=" + skillId + ": " + ex.getMessage());
                    return null;
                });

        plugin.getServer().getPluginManager().callEvent(new SkillXPGainEvent(uuid, skill, after - before, after));

        int levelBefore = snapshot.levelFromXp(before);
        int levelAfter  = snapshot.levelFromXp(after);
        if (levelAfter > levelBefore) {
            plugin.getServer().getPluginManager().callEvent(
                    new SkillLevelUpEvent(uuid, skill, levelBefore, levelAfter));
            showLevelUpEffects(player, skill, levelAfter, snapshot);
        }

        int levelCap = snapshot.levelCap();
        if (levelBefore < levelCap && levelAfter >= levelCap) {
            plugin.getServer().getPluginManager().callEvent(new SkillMaxLevelEvent(uuid, skill, levelCap));
        }
    }

    @Override
    public CompletableFuture<Long> getXP(UUID uuid, String skillId) {
        return repository.loadAll(uuid)
                .thenApply(data -> data.getOrDefault(skillId, 0L));
    }

    /**
     * Directly sets a player's XP for a skill, clamping to [0, XP cap].
     * Updates the in-memory cache and writes through to the database.
     * Does not fire events or show level-up effects.
     *
     * @param uuid    the target player's UUID
     * @param skillId the skill to modify
     * @param amount  the desired XP value; clamped to [0, cap]
     * @return the actual XP value after clamping
     */
    public CompletableFuture<Long> adminSetXP(UUID uuid, String skillId, long amount) {
        SkillsConfig snapshot = config;
        long clamped = Math.clamp(amount, 0L, snapshot.xpForLevel(snapshot.levelCap()));
        cache.computeIfAbsent(uuid, _ -> new ConcurrentHashMap<>()).put(skillId, clamped);
        return repository.upsertXP(uuid, skillId, clamped).thenApply(_ -> clamped);
    }

    /**
     * Adds XP to a player's skill, clamping at the XP cap.
     * Updates the in-memory cache and writes through to the database.
     * Does not fire events or show level-up effects.
     *
     * @param uuid    the target player's UUID
     * @param skillId the skill to modify
     * @param amount  the amount of XP to add; must be positive
     * @return the resulting XP total
     */
    public CompletableFuture<Long> adminAddXP(UUID uuid, String skillId, long amount) {
        SkillsConfig snapshot = config;
        long cap = snapshot.xpForLevel(snapshot.levelCap());
        Map<String, Long> playerSkills = cache.computeIfAbsent(uuid, _ -> new ConcurrentHashMap<>());
        long after = Math.min(playerSkills.getOrDefault(skillId, 0L) + amount, cap);
        playerSkills.put(skillId, after);
        return repository.upsertXP(uuid, skillId, after).thenApply(_ -> after);
    }

    /**
     * Subtracts XP from a player's skill, clamping at zero.
     * Updates the in-memory cache and writes through to the database.
     * Does not fire events or show level-up effects.
     *
     * @param uuid    the target player's UUID
     * @param skillId the skill to modify
     * @param amount  the amount of XP to remove; must be positive
     * @return the resulting XP total
     */
    public CompletableFuture<Long> adminRemoveXP(UUID uuid, String skillId, long amount) {
        Map<String, Long> playerSkills = cache.computeIfAbsent(uuid, _ -> new ConcurrentHashMap<>());
        long after = Math.max(0L, playerSkills.getOrDefault(skillId, 0L) - amount);
        playerSkills.put(skillId, after);
        return repository.upsertXP(uuid, skillId, after).thenApply(_ -> after);
    }

    @Override
    public boolean isAtCap(UUID uuid, String skillId) {
        SkillsConfig snapshot = config;
        return getCachedXP(uuid, skillId) >= snapshot.xpForLevel(snapshot.levelCap());
    }

    @Override
    public Collection<Skill> getSkills() {
        return registry.all();
    }

    @Override
    public Optional<Skill> getSkill(String skillId) {
        return registry.get(skillId);
    }

    private void showLevelUpEffects(Player player, Skill skill, int newLevel, SkillsConfig snapshot) {
        SkillsConfig.LevelUpConfig lu = snapshot.levelUp();

        if (lu.soundEnabled()) {
            NamespacedKey key = NamespacedKey.fromString(lu.soundName());
            Sound sound = key != null ? Registry.SOUNDS.get(key) : null;
            if (sound != null) {
                player.playSound(player.getLocation(), sound, lu.soundVolume(), lu.soundPitch());
            } else {
                plugin.getLogger().warning("Unknown sound key in config: " + lu.soundName());
            }
        }

        if (lu.titleEnabled()) {
            String titleStr = lu.title()
                    .replace("{skill}", skill.getDisplayName())
                    .replace("{level}", String.valueOf(newLevel));
            String subtitleStr = lu.subtitle()
                    .replace("{skill}", skill.getDisplayName())
                    .replace("{level}", String.valueOf(newLevel));

            player.showTitle(Title.title(
                    MINI.deserialize(titleStr),
                    MINI.deserialize(subtitleStr),
                    Title.Times.times(
                            Duration.ofMillis(lu.fadeIn() * 50L),
                            Duration.ofMillis(lu.stay() * 50L),
                            Duration.ofMillis(lu.fadeOut() * 50L)
                    )
            ));
        }
    }
}
