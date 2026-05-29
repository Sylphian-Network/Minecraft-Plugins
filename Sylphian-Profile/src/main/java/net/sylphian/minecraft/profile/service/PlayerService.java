package net.sylphian.minecraft.profile.service;

import net.sylphian.minecraft.profile.db.api.IPlayerRepository;
import net.sylphian.minecraft.profile.db.api.ISessionRepository;
import net.sylphian.minecraft.profile.db.models.PlayerModel;
import net.sylphian.minecraft.profile.UserProfile;
import net.sylphian.minecraft.profile.utils.ProfileManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerService {
    private final IPlayerRepository playerRepository;
    private final ISessionRepository sessionRepository;
    private final ProfileManager profileManager;

    public PlayerService(IPlayerRepository playerRepository,
                         ISessionRepository sessionRepository,
                         ProfileManager profileManager) {
        this.playerRepository = playerRepository;
        this.sessionRepository = sessionRepository;
        this.profileManager = profileManager;
    }

    public CompletableFuture<UserProfile> handleJoin(UUID uuid, String mcUsername) {
        long now = Instant.now().getEpochSecond();
        return playerRepository.findByUuid(uuid).thenCompose(playerOpt -> {
            if (playerOpt.isPresent()) {
                PlayerModel p = playerOpt.get();
                PlayerModel updated = new PlayerModel(uuid, p.xfUserId(), p.mcUsername(),
                        p.forumUsername(), p.firstJoined(), now, p.playtime(), true);
                return playerRepository.update(updated).thenApply(v -> updated);
            } else {
                PlayerModel inserted = new PlayerModel(uuid, null, mcUsername,
                        null, now, now, 0, true);
                return playerRepository.insert(inserted).thenApply(v -> inserted);
            }
        }).thenCompose(playerModel -> sessionRepository.findOpenByUuid(uuid).thenCompose(openSessionOpt -> {
            if (openSessionOpt.isPresent()) {
                var openSession = openSessionOpt.get();
                long duration = Math.max(0, now - openSession.joinedAt());
                return sessionRepository.close(openSession.sessionId(), now, duration);
            }
            return CompletableFuture.completedFuture(null);
        }).thenCompose(v ->
                sessionRepository.open(uuid, now)
        ).thenApply(sessionId -> {
            UserProfile profile = new UserProfile(uuid, playerModel.xfUserId(),
                    playerModel.mcUsername(), playerModel.forumUsername(),
                    playerModel.firstJoined(), playerModel.lastSeen(),
                    playerModel.playtime(), new ArrayList<>());
            profileManager.cacheProfile(profile);
            return profile;
        }));
    }

    public CompletableFuture<Void> handleQuit(UUID uuid) {
        long now = Instant.now().getEpochSecond();
        return sessionRepository.findOpenByUuid(uuid).thenCompose(sessionOpt -> {
            if (sessionOpt.isPresent()) {
                var session = sessionOpt.get();
                long duration = now - session.joinedAt();
                return sessionRepository.close(session.sessionId(), now, duration).thenCompose(v ->
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
        }).thenAccept(v -> profileManager.invalidate(uuid));
    }

    public CompletableFuture<Long> getTotalPlaytime(UUID uuid) {
        return playerRepository.findByUuid(uuid).thenCompose(playerOpt -> {
            if (playerOpt.isEmpty()) return CompletableFuture.completedFuture(0L);
            long storedPlaytime = playerOpt.get().playtime();
            return sessionRepository.findOpenByUuid(uuid).thenApply(sessionOpt -> {
                long currentSessionDuration = sessionOpt
                        .map(session -> Instant.now().getEpochSecond() - session.joinedAt())
                        .orElse(0L);
                return storedPlaytime + currentSessionDuration;
            });
        });
    }
}
