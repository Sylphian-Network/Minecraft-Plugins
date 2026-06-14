package net.sylphian.minecraft.economy.db.repositories;

import net.sylphian.minecraft.economy.db.api.IEconomyRepository;
import net.sylphian.minecraft.economy.db.dao.EconomyDao;
import org.jdbi.v3.core.Jdbi;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository implementation for economy balances using JDBI.
 *
 * <p>Wraps blocking database calls in {@link CompletableFuture} on the shared
 * database thread pool. Compound operations run inside a single transaction so
 * they commit or roll back as a unit.</p>
 */
public class EconomyRepository implements IEconomyRepository {
    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * Constructs a new EconomyRepository.
     *
     * @param jdbi     the JDBI instance for database access
     * @param executor the executor service for async operations
     */
    public EconomyRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(EconomyDao.class, dao ->
                        dao.findBalance(uuid.toString()).orElse(BigDecimal.ZERO)
                ), executor);
    }

    @Override
    public CompletableFuture<Void> ensureAccount(UUID uuid, BigDecimal starting) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(EconomyDao.class, dao ->
                        dao.ensureAccount(uuid.toString(), starting)
                ), executor);
    }

    @Override
    public CompletableFuture<Void> deposit(UUID uuid, BigDecimal amount) {
        return CompletableFuture.runAsync(() ->
                jdbi.useTransaction(handle -> {
                    EconomyDao dao = handle.attach(EconomyDao.class);
                    dao.ensureAccount(uuid.toString(), BigDecimal.ZERO);
                    dao.credit(uuid.toString(), amount);
                }), executor);
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID uuid, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.inTransaction(handle -> {
                    EconomyDao dao = handle.attach(EconomyDao.class);
                    dao.ensureAccount(uuid.toString(), BigDecimal.ZERO);
                    // 1 row affected => debit applied; 0 => insufficient funds.
                    return dao.debitIfSufficient(uuid.toString(), amount) == 1;
                }), executor);
    }

    @Override
    public CompletableFuture<Void> set(UUID uuid, BigDecimal amount) {
        return CompletableFuture.runAsync(() ->
                jdbi.useTransaction(handle -> {
                    EconomyDao dao = handle.attach(EconomyDao.class);
                    dao.ensureAccount(uuid.toString(), BigDecimal.ZERO);
                    dao.setBalance(uuid.toString(), amount);
                }), executor);
    }

    @Override
    public CompletableFuture<Boolean> transfer(UUID from, UUID to, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.inTransaction(handle -> {
                    EconomyDao dao = handle.attach(EconomyDao.class);
                    dao.ensureAccount(from.toString(), BigDecimal.ZERO);
                    dao.ensureAccount(to.toString(), BigDecimal.ZERO);

                    boolean debited = dao.debitIfSufficient(from.toString(), amount) == 1;
                    if (!debited) {
                        return false;
                    }
                    dao.credit(to.toString(), amount);
                    return true;
                }), executor);
    }
}
