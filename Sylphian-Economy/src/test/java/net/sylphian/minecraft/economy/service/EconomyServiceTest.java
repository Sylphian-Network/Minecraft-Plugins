package net.sylphian.minecraft.economy.service;

import net.sylphian.minecraft.economy.db.api.IEconomyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** Covers normalization, sign validation, the self-transfer guard, and publish rules. */
class EconomyServiceTest {

    private final UUID player = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    private FakeRepository repository;
    private List<UUID> published;
    private EconomyService service;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        published = new ArrayList<>();
        service = new EconomyService(repository, new BigDecimal("10.005"), published::add);
    }

    @Test
    void amountsRoundHalfUpToTwoDecimalsBeforeReachingRepository() {
        service.deposit(player, new BigDecimal("0.005")).join();

        assertThat(repository.lastAmount).isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void nullAmountIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.deposit(player, null));
    }

    @Test
    void depositWithdrawAndTransferRejectZeroAndNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.deposit(player, BigDecimal.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> service.deposit(player, new BigDecimal("-1")));
        assertThatIllegalArgumentException().isThrownBy(() -> service.withdraw(player, BigDecimal.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> service.transfer(player, other, BigDecimal.ZERO));
        // 0.004 rounds to 0.00, so it must also be rejected.
        assertThatIllegalArgumentException().isThrownBy(() -> service.deposit(player, new BigDecimal("0.004")));
        assertThat(repository.calls).isEmpty();
    }

    @Test
    void setAllowsZeroButRejectsNegative() {
        service.set(player, BigDecimal.ZERO).join();
        assertThat(repository.lastAmount).isEqualByComparingTo("0");

        assertThatIllegalArgumentException().isThrownBy(() -> service.set(player, new BigDecimal("-0.01")));
    }

    @Test
    void selfTransferReturnsFalseWithoutTouchingRepository() {
        assertThat(service.transfer(player, player, BigDecimal.ONE).join()).isFalse();

        assertThat(repository.calls).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    void depositAndSetPublishBalanceChange() {
        service.deposit(player, BigDecimal.ONE).join();
        service.set(other, BigDecimal.TEN).join();

        assertThat(published).containsExactly(player, other);
    }

    @Test
    void withdrawPublishesOnlyOnSuccess() {
        repository.withdrawResult = false;
        assertThat(service.withdraw(player, BigDecimal.ONE).join()).isFalse();
        assertThat(published).isEmpty();

        repository.withdrawResult = true;
        assertThat(service.withdraw(player, BigDecimal.ONE).join()).isTrue();
        assertThat(published).containsExactly(player);
    }

    @Test
    void transferPublishesBothPartiesOnlyOnSuccess() {
        repository.transferResult = false;
        assertThat(service.transfer(player, other, BigDecimal.ONE).join()).isFalse();
        assertThat(published).isEmpty();

        repository.transferResult = true;
        assertThat(service.transfer(player, other, BigDecimal.ONE).join()).isTrue();
        assertThat(published).containsExactly(player, other);
    }

    @Test
    void loadSeedsAccountWithNormalizedStartingBalance() {
        service.load(player).join();
        assertThat(repository.lastStarting).isEqualTo(new BigDecimal("10.01"));

        service.setStartingBalance(new BigDecimal("5.999"));
        service.load(player).join();
        assertThat(repository.lastStarting).isEqualTo(new BigDecimal("6.00"));
    }

    @Test
    void hasIsTrueAtExactlyTheNeededAmount() {
        repository.balances.put(player, new BigDecimal("10.00"));

        assertThat(service.has(player, new BigDecimal("10.00")).join()).isTrue();
        assertThat(service.has(player, new BigDecimal("10.01")).join()).isFalse();
    }

    /** Records calls and amounts; results are configurable per test. */
    private static final class FakeRepository implements IEconomyRepository {
        final Map<UUID, BigDecimal> balances = new HashMap<>();
        final List<String> calls = new ArrayList<>();
        BigDecimal lastAmount;
        BigDecimal lastStarting;
        boolean withdrawResult = true;
        boolean transferResult = true;

        @Override
        public CompletableFuture<BigDecimal> getBalance(UUID uuid) {
            calls.add("getBalance");
            return CompletableFuture.completedFuture(balances.getOrDefault(uuid, BigDecimal.ZERO));
        }

        @Override
        public CompletableFuture<Void> ensureAccount(UUID uuid, BigDecimal starting) {
            calls.add("ensureAccount");
            lastStarting = starting;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> deposit(UUID uuid, BigDecimal amount) {
            calls.add("deposit");
            lastAmount = amount;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> withdraw(UUID uuid, BigDecimal amount) {
            calls.add("withdraw");
            lastAmount = amount;
            return CompletableFuture.completedFuture(withdrawResult);
        }

        @Override
        public CompletableFuture<Void> set(UUID uuid, BigDecimal amount) {
            calls.add("set");
            lastAmount = amount;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> transfer(UUID from, UUID to, BigDecimal amount) {
            calls.add("transfer");
            lastAmount = amount;
            return CompletableFuture.completedFuture(transferResult);
        }
    }
}
