package net.sylphian.verify.paper.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sylphian.verify.paper.api.VerifyService;
import org.bukkit.configuration.file.FileConfiguration;
import net.sylphian.verify.paper.model.PlayerIdentity;
import net.sylphian.verify.paper.model.VerificationResult;
import net.sylphian.verify.paper.util.MessageUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VerifyManager {
    private final VerifyService verifyService;
    private final FileConfiguration config;

    private final Cache<UUID, Integer> uuidAttempts;
    private final Cache<String, Integer> ipAttempts;
    private final Cache<UUID, Long> uuidCooldown;
    private final Cache<String, Long> ipCooldown;
    private final Cache<UUID, Integer> strikes;

    public VerifyManager(VerifyService verifyService, FileConfiguration config) {
        this.verifyService = verifyService;
        this.config = config;

        this.uuidAttempts = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("attempt_expiry_minutes", 30), TimeUnit.MINUTES)
                .build();

        this.ipAttempts = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("attempt_expiry_minutes", 30), TimeUnit.MINUTES)
                .build();

        this.uuidCooldown = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("cooldown_minutes", 15), TimeUnit.MINUTES)
                .build();

        this.ipCooldown = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("cooldown_minutes", 15), TimeUnit.MINUTES)
                .build();

        this.strikes = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("attempt_expiry_minutes", 30), TimeUnit.MINUTES)
                .build();
    }

    public CompletableFuture<VerificationResult> checkPlayer(UUID uuid, String ip) {
        Long uuidExpiry = uuidCooldown.getIfPresent(uuid);
        if (uuidExpiry != null && uuidExpiry > System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(VerificationResult.denied(MessageUtils.buildCooldownMessage(uuidExpiry, config), null));
        }

        Long ipExpiry = ipCooldown.getIfPresent(ip);
        if (ipExpiry != null && ipExpiry > System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(VerificationResult.denied(MessageUtils.buildCooldownMessage(ipExpiry, config), null));
        }

        return verifyService.checkVerification(uuid).thenApply(response -> {
            PlayerIdentity identity = PlayerIdentity.from(response, uuid);
            if (response.isAllowed()) {
                resetAttempts(uuid, ip);
                return VerificationResult.allowed(identity);
            } else {
                handleFailedAttempt(uuid, ip);
                return VerificationResult.denied(MessageUtils.buildKickMessage(response, config), identity);
            }
        });
    }

    private void handleFailedAttempt(UUID uuid, String ip) {
        int uAttempts = uuidAttempts.get(uuid, k -> 0) + 1;
        uuidAttempts.put(uuid, uAttempts);
        if (uAttempts >= config.getInt("uuid_attempt_limit", 5)) {
            uuidCooldown.put(uuid, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(config.getInt("cooldown_minutes", 15)));
        }

        int iAttempts = ipAttempts.get(ip, k -> 0) + 1;
        ipAttempts.put(ip, iAttempts);
        if (iAttempts >= config.getInt("ip_attempt_limit", 10)) {
            ipCooldown.put(ip, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(config.getInt("cooldown_minutes", 15)));
        }
    }

    private void resetAttempts(UUID uuid, String ip) {
        uuidAttempts.invalidate(uuid);
        ipAttempts.invalidate(ip);
        uuidCooldown.invalidate(uuid);
        ipCooldown.invalidate(ip);
    }

    public boolean addStrike(UUID uuid) {
        int s = strikes.get(uuid, k -> 0) + 1;
        strikes.put(uuid, s);
        return s >= config.getInt("max_strikes", 3);
    }

    public void resetStrikes(UUID uuid) {
        strikes.invalidate(uuid);
    }
}
