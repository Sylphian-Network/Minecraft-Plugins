package net.sylphian.velocity.verify.manager;

import net.sylphian.velocity.verify.api.VerifyService;
import net.sylphian.velocity.verify.api.model.VerificationReason;
import net.sylphian.velocity.verify.api.model.VerificationResponse;
import net.sylphian.velocity.verify.model.VerificationResult;
import net.sylphian.velocity.verify.model.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers rate-limiting, the technical-failure/logical-denial split, and admin reset/status. */
class VerifyManagerTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final String IP = "127.0.0.1";

    private FakeVerifyService service;
    private VerifyManager manager;

    @BeforeEach
    void setUp() {
        service = new FakeVerifyService();
        manager = new VerifyManager(service, baseConfig());
    }

    private static Map<String, Object> baseConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("uuid_attempt_limit", 3);
        config.put("ip_attempt_limit", 5);
        config.put("max_strikes", 2);
        config.put("cooldown_minutes", 15);
        config.put("attempt_expiry_minutes", 30);
        config.put("strike_on_api_failure", true);
        return config;
    }

    @Test
    void successfulVerificationClearsAttemptsAndStrikes() {
        service.respond(PLAYER, new VerificationResponse(false, VerificationReason.UUID_NOT_LINKED));
        manager.checkPlayer(PLAYER, IP).join();
        assertThat(manager.getStatus(PLAYER).attempts()).isEqualTo(1);

        service.respond(PLAYER, new VerificationResponse(true, null));
        VerificationResult result = manager.checkPlayer(PLAYER, IP).join();

        assertThat(result.allowed()).isTrue();
        assertThat(result.identity()).isNotNull();
        assertThat(result.identity().uuid()).isEqualTo(PLAYER);
        assertThat(manager.getStatus(PLAYER).attempts()).isZero();
    }

    @Test
    void logicalDenialsTriggerCooldownAfterLimitAndThenShortCircuit() {
        service.respond(PLAYER, new VerificationResponse(false, VerificationReason.UUID_NOT_LINKED));

        for (int i = 0; i < 3; i++) {
            assertThat(manager.checkPlayer(PLAYER, IP).join().allowed()).isFalse();
        }
        assertThat(manager.getStatus(PLAYER).cooldownRemainingMillis()).isGreaterThan(0);

        int callsBeforeCooldown = service.checkVerificationCalls;
        VerificationResult blocked = manager.checkPlayer(PLAYER, IP).join();

        assertThat(blocked.allowed()).isFalse();
        assertThat(service.checkVerificationCalls).isEqualTo(callsBeforeCooldown);
    }

    @Test
    void technicalFailuresFailOpenAndDoNotTriggerBruteForceCooldown() {
        service.respond(PLAYER, new VerificationResponse(false, VerificationReason.API_ERROR));

        // max_strikes = 2: the first technical failure fails open, the second reaches the limit.
        VerificationResult first = manager.checkPlayer(PLAYER, IP).join();
        assertThat(first.allowed()).isTrue();
        assertThat(first.identity()).isNull();

        VerificationResult second = manager.checkPlayer(PLAYER, IP).join();
        assertThat(second.allowed()).isFalse();

        // Neither call should have touched the brute-force attempt/cooldown counters.
        assertThat(manager.getStatus(PLAYER).attempts()).isZero();
        assertThat(manager.getStatus(PLAYER).cooldownRemainingMillis()).isZero();
    }

    @Test
    void ipAttemptLimitBlocksAnyUuidSharingThatIp() {
        Map<String, Object> tightIpConfig = new HashMap<>(baseConfig());
        tightIpConfig.put("ip_attempt_limit", 2);
        manager = new VerifyManager(service, tightIpConfig);

        service.respond(PLAYER, new VerificationResponse(false, VerificationReason.UUID_NOT_LINKED));
        manager.checkPlayer(PLAYER, IP).join();
        manager.checkPlayer(PLAYER, IP).join();

        UUID otherPlayer = UUID.randomUUID();
        service.respond(otherPlayer, new VerificationResponse(true, null));
        VerificationResult result = manager.checkPlayer(otherPlayer, IP).join();

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void periodicBatchWithNoPlayersReturnsEmptyMap() {
        assertThat(manager.checkPeriodicBatch(List.of()).join()).isEmpty();
        assertThat(manager.checkPeriodicBatch(null).join()).isEmpty();
    }

    @Test
    void periodicBatchFailureFailsOpenThenDeniesAfterStrikeLimit() {
        service.batchFails();

        VerificationResult first = manager.checkPeriodicBatch(List.of(PLAYER)).join().get(PLAYER);
        assertThat(first.allowed()).isTrue();

        VerificationResult second = manager.checkPeriodicBatch(List.of(PLAYER)).join().get(PLAYER);
        assertThat(second.allowed()).isFalse();
    }

    @Test
    void periodicBatchTreatsMissingEntryAsUnchanged() {
        VerificationResult result = manager.checkPeriodicBatch(List.of(PLAYER)).join().get(PLAYER);

        assertThat(result.allowed()).isTrue();
        assertThat(result.identity()).isNull();
    }

    @Test
    void resetAllClearsAttemptsCooldownAndStrikes() {
        service.respond(PLAYER, new VerificationResponse(false, VerificationReason.UUID_NOT_LINKED));
        manager.checkPlayer(PLAYER, IP).join();
        manager.checkPlayer(PLAYER, IP).join();
        manager.checkPlayer(PLAYER, IP).join();
        assertThat(manager.getStatus(PLAYER).cooldownRemainingMillis()).isGreaterThan(0);

        manager.resetAll(PLAYER);

        VerificationStatus status = manager.getStatus(PLAYER);
        assertThat(status.attempts()).isZero();
        assertThat(status.strikes()).isZero();
        assertThat(status.cooldownRemainingMillis()).isZero();
    }

    @Test
    void reloadConfigChangesThresholdsLive() {
        Map<String, Object> tighter = new HashMap<>(baseConfig());
        tighter.put("uuid_attempt_limit", 1);
        manager.reloadConfig(tighter);

        service.respond(PLAYER, new VerificationResponse(false, VerificationReason.UUID_NOT_LINKED));
        VerificationResult result = manager.checkPlayer(PLAYER, IP).join();

        assertThat(result.allowed()).isFalse();
        assertThat(manager.getStatus(PLAYER).cooldownRemainingMillis()).isGreaterThan(0);
    }

    /** Fakes the API boundary so VerifyManager's rate-limit logic can be tested without network I/O. */
    private static final class FakeVerifyService extends VerifyService {
        private final Map<UUID, VerificationResponse> responses = new HashMap<>();
        private final Map<String, VerificationResponse> batchResponses = new HashMap<>();
        private boolean batchFails;
        int checkVerificationCalls;

        FakeVerifyService() {
            super(null);
        }

        void respond(UUID uuid, VerificationResponse response) {
            responses.put(uuid, response);
        }

        void batchFails() {
            this.batchFails = true;
        }

        @Override
        public CompletableFuture<VerificationResponse> checkVerification(UUID uuid) {
            checkVerificationCalls++;
            VerificationResponse response = responses.get(uuid);
            return CompletableFuture.completedFuture(
                    response != null ? response : new VerificationResponse(false, VerificationReason.UUID_NOT_LINKED));
        }

        @Override
        public CompletableFuture<Map<String, VerificationResponse>> checkVerificationBatch(Collection<UUID> uuids) {
            if (batchFails) {
                return CompletableFuture.failedFuture(new RuntimeException("simulated batch failure"));
            }
            return CompletableFuture.completedFuture(batchResponses);
        }
    }
}
