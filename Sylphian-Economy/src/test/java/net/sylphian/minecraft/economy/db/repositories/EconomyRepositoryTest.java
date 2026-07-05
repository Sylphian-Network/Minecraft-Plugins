package net.sylphian.minecraft.economy.db.repositories;

import net.sylphian.minecraft.economy.db.migrations.Migration001CreateBalances;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the real SQL (guards, transactions, INSERT IGNORE) against H2 in MariaDB mode. */
class EconomyRepositoryTest {

    private final UUID payer = UUID.randomUUID();
    private final UUID payee = UUID.randomUUID();

    private EconomyRepository repository;

    @BeforeEach
    void setUp() {
        Jdbi jdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
                .installPlugin(new SqlObjectPlugin());
        jdbi.useHandle(handle -> new Migration001CreateBalances().up(handle));
        repository = new EconomyRepository(jdbi, new DirectExecutorService());
    }

    private BigDecimal balance(UUID uuid) {
        return repository.getBalance(uuid).join();
    }

    @Test
    void balanceIsZeroWithoutAccountRow() {
        assertThat(balance(payer)).isEqualByComparingTo("0");
    }

    @Test
    void ensureAccountSeedsStartingBalanceOnlyOnce() {
        repository.ensureAccount(payer, new BigDecimal("100.00")).join();
        repository.ensureAccount(payer, new BigDecimal("500.00")).join();

        assertThat(balance(payer)).isEqualByComparingTo("100.00");
    }

    @Test
    void depositCreatesMissingAccountAndCredits() {
        repository.deposit(payer, new BigDecimal("25.00")).join();

        assertThat(balance(payer)).isEqualByComparingTo("25.00");
    }

    @Test
    void depositAddsToExistingBalance() {
        repository.ensureAccount(payer, new BigDecimal("10.00")).join();
        repository.deposit(payer, new BigDecimal("2.50")).join();

        assertThat(balance(payer)).isEqualByComparingTo("12.50");
    }

    @Test
    void withdrawFailsWhenFundsInsufficientAndLeavesBalanceUntouched() {
        repository.ensureAccount(payer, new BigDecimal("10.00")).join();

        assertThat(repository.withdraw(payer, new BigDecimal("10.01")).join()).isFalse();
        assertThat(balance(payer)).isEqualByComparingTo("10.00");
    }

    @Test
    void withdrawOfExactBalanceSucceeds() {
        repository.ensureAccount(payer, new BigDecimal("10.00")).join();

        assertThat(repository.withdraw(payer, new BigDecimal("10.00")).join()).isTrue();
        assertThat(balance(payer)).isEqualByComparingTo("0");
    }

    @Test
    void transferMovesAmountBetweenPlayers() {
        repository.ensureAccount(payer, new BigDecimal("100.00")).join();

        assertThat(repository.transfer(payer, payee, new BigDecimal("40.00")).join()).isTrue();
        assertThat(balance(payer)).isEqualByComparingTo("60.00");
        assertThat(balance(payee)).isEqualByComparingTo("40.00");
    }

    @Test
    void insufficientTransferChangesNeitherBalance() {
        repository.ensureAccount(payer, new BigDecimal("10.00")).join();

        assertThat(repository.transfer(payer, payee, new BigDecimal("20.00")).join()).isFalse();
        assertThat(balance(payer)).isEqualByComparingTo("10.00");
        assertThat(balance(payee)).isEqualByComparingTo("0");
    }

    @Test
    void setOverwritesBalance() {
        repository.ensureAccount(payer, new BigDecimal("10.00")).join();
        repository.set(payer, new BigDecimal("1234.56")).join();

        assertThat(balance(payer)).isEqualByComparingTo("1234.56");
    }

    @Test
    void twoDecimalPrecisionSurvivesRoundTrip() {
        repository.deposit(payer, new BigDecimal("0.01")).join();
        repository.deposit(payer, new BigDecimal("99999999.99")).join();

        assertThat(balance(payer)).isEqualByComparingTo("100000000.00");
    }

    /** Runs tasks on the calling thread so futures complete synchronously. */
    private static final class DirectExecutorService extends AbstractExecutorService {
        @Override public void execute(Runnable command) { command.run(); }
        @Override public void shutdown() { }
        @Override public List<Runnable> shutdownNow() { return List.of(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return false; }
    }
}
