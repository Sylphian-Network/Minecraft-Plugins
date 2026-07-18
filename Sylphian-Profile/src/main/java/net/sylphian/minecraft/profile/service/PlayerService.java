package net.sylphian.minecraft.profile.service;

import net.sylphian.minecraft.profile.db.api.IPlayerRepository;
import net.sylphian.minecraft.profile.db.api.ISessionRepository;
import net.sylphian.minecraft.profile.db.models.PlayerModel;
import net.sylphian.minecraft.profile.UserProfile;
import net.sylphian.minecraft.profile.utils.ProfileManager;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Core service for player management logic.
 * Orchestrates player joins, quits, and playtime calculations by coordinating
 * between player and session repositories and the in-memory profile manager.
 */
public class PlayerService {
    private final IPlayerRepository playerRepository;
    private final ISessionRepository sessionRepository;
    private final ProfileManager profileManager;

    /**
     * Constructs a new PlayerService.
     *
     * @param playerRepository  the player data repository
     * @param sessionRepository the session history repository
     * @param profileManager    the in-memory profile cache
     */
    public PlayerService(IPlayerRepository playerRepository,
                         ISessionRepository sessionRepository,
                         ProfileManager profileManager) {
        this.playerRepository = playerRepository;
        this.sessionRepository = sessionRepository;
        this.profileManager = profileManager;
    }

    /**
     * Handles a player joining the server.
     * Loads or creates the player's record in the database, closes any lingering
     * open sessions from previous crashes, starts a new session, and caches
     * the profile in memory.
     *
     * @param uuid       the player's UUID
     * @param mcUsername the player's current username
     * @return a future containing the loaded UserProfile
     */
    public CompletableFuture<UserProfile> handleJoin(UUID uuid, String mcUsername) {
        long now = Instant.now().getEpochSecond();
        // First, check if player exists in the sylphian_profile_players table
        return playerRepository.findByUuid(uuid).thenCompose(playerOpt -> {
            if (playerOpt.isPresent()) {
                // Update existing player record with new last_seen and online status
                PlayerModel p = playerOpt.get();
                PlayerModel updated = new PlayerModel(uuid, p.xfUserId(), p.mcUsername(),
                        p.forumUsername(), p.firstJoined(), now, p.playtime(), true);
                return playerRepository.update(updated).thenApply(v -> updated);
            } else {
                // Create new player record if this is their first time joining
                PlayerModel inserted = new PlayerModel(uuid, null, mcUsername,
                        null, now, now, 0, true);
                return playerRepository.insert(inserted).thenApply(v -> inserted);
            }
        }).thenCompose(playerModel -> sessionRepository.findOpenByUuid(uuid).thenCompose(openSessionOpt -> {
            // Close any old open sessions (e.g., if the server crashed and didn't record a quit)
            if (openSessionOpt.isPresent()) {
                var openSession = openSessionOpt.get();
                long duration = Math.max(0, now - openSession.joinedAt());
                return sessionRepository.close(openSession.sessionId(), now, duration);
            }
            return CompletableFuture.completedFuture(null);
        }).thenCompose(v ->
                // Start a fresh session for this login
                sessionRepository.open(uuid, now)
        ).thenApply(sessionId -> {
            // Build and cache the final UserProfile for use by other systems
            UserProfile profile = new UserProfile(uuid, playerModel.xfUserId(),
                    playerModel.mcUsername(), playerModel.forumUsername(),
                    playerModel.firstJoined(), playerModel.lastSeen(),
                    playerModel.playtime(), new ArrayList<>());
            profileManager.cacheProfile(profile);
            return profile;
        }));
    }

    /**
     * Handles a player quitting the server.
     * Closes their active session, updates their cumulative playtime and offline status
     * in the database, and removes their profile from the in-memory cache.
     *
     * @param uuid the player's UUID
     * @return a future that completes when all quit logic is finished
     */
    public CompletableFuture<Void> handleQuit(UUID uuid) {
        long now = Instant.now().getEpochSecond();
        // Find and close the currently active session
        return sessionRepository.findOpenByUuid(uuid).thenCompose(sessionOpt -> {
            if (sessionOpt.isPresent()) {
                var session = sessionOpt.get();
                long duration = now - session.joinedAt();
                return sessionRepository.close(session.sessionId(), now, duration).thenCompose(v ->
                        // Update player's total playtime and online status
                        playerRepository.findByUuid(uuid).thenCompose(playerOpt -> {
                            if (playerOpt.isPresent()) {
                                PlayerModel p = playerOpt.get();
                                PlayerModel updated = new PlayerModel(uuid, p.xfUserId(), p.mcUsername(),
                                        p.forumUsername(), p.firstJoined(), now, p.playtime() + duration, false);
                                return playerRepository.update(updated);
                            }
                            return CompletableFuture.completedFuture(null);
                        })
                );
            }
            return CompletableFuture.completedFuture(null);
        }).thenAccept(v -> profileManager.invalidate(uuid)); // Remove from memory cache
    }

    /**
     * Ensures a player row exists in {@code sylphian_profile_players}.
     * If the row exists, updates {@code xf_user_id}, {@code mc_username}, {@code forum_username}
     * and {@code last_seen}; otherwise inserts a new row with default playtime and offline status.
     *
     * <p>Executes asynchronously on the database executor.</p>
     *
     * @param uuid          the player's Mojang UUID
     * @param xfUserId      the linked XenForo user ID, or {@code null} if unknown
     * @param mcUsername    the player's current Minecraft username
     * @param forumUsername the player's XenForo username, or {@code null} if unknown
     * @return a future that completes when the upsert is done
     */
    public CompletableFuture<Void> ensurePlayerExists(UUID uuid, @Nullable Integer xfUserId,
                                                      String mcUsername, @Nullable String forumUsername) {
        long now = Instant.now().getEpochSecond();
        return playerRepository.findByUuid(uuid).thenCompose(opt -> {
            if (opt.isPresent()) {
                PlayerModel existing = opt.get();
                PlayerModel updated = new PlayerModel(
                        uuid, xfUserId, mcUsername, forumUsername,
                        existing.firstJoined(), now, existing.playtime(), existing.isOnline());
                return playerRepository.update(updated);
            } else {
                PlayerModel inserted = new PlayerModel(
                        uuid, xfUserId, mcUsername, forumUsername,
                        now, now, 0, false);
                return playerRepository.insert(inserted);
            }
        });
    }

    /**
     * Calculates the total cumulative playtime for a player.
     * This includes both stored playtime from previous sessions and the
     * duration of their currently active session if they are online.
     *
     * @param uuid the player's UUID
     * @return a future containing total playtime in seconds
     */
    public CompletableFuture<Long> getTotalPlaytime(UUID uuid) {
        return playerRepository.findByUuid(uuid).thenCompose(playerOpt -> {
            if (playerOpt.isEmpty()) return CompletableFuture.completedFuture(0L);
            long storedPlaytime = playerOpt.get().playtime();
            // Add duration of current session if player is online
            return sessionRepository.findOpenByUuid(uuid).thenApply(sessionOpt -> {
                long currentSessionDuration = sessionOpt
                        .map(session -> Instant.now().getEpochSecond() - session.joinedAt())
                        .orElse(0L);
                return storedPlaytime + currentSessionDuration;
            });
        });
    }
}
