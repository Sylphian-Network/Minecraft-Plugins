package net.sylphian.minecraft.economy.service;

import net.sylphian.minecraft.economy.api.EconomyAPI;
import net.sylphian.minecraft.economy.db.api.IEconomyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Business-logic implementation of {@link EconomyAPI}.
 *
 * <p>Validates and normalises amounts (positive, two decimal places) before
 * delegating persistence to {@link IEconomyRepository}.</p>
 */
public class EconomyService implements EconomyAPI {

    private final IEconomyRepository repository;
    private final BalanceChangePublisher publisher;
    private BigDecimal startingBalance;

    /**
     * @param repository      the persistence layer
     * @param startingBalance the balance seeded for a player's first-ever account row
     * @param publisher       announces balance changes (e.g. fires a Bukkit event)
     */
    public EconomyService(IEconomyRepository repository, BigDecimal startingBalance,
                          BalanceChangePublisher publisher) {
        this.repository = repository;
        this.startingBalance = normalize(startingBalance, false);
        this.publisher = publisher;
    }

    /**
     * Updates the starting balance applied to new accounts (e.g. after a config reload).
     *
     * @param startingBalance the new starting balance
     */
    public void setStartingBalance(BigDecimal startingBalance) {
        this.startingBalance = normalize(startingBalance, false);
    }

    /**
     * Ensures a player has an account row, seeded with the configured starting
     * balance. Intended to be called when a player joins.
     *
     * @param uuid the player's UUID
     * @return a future that completes when the account is guaranteed to exist
     */
    public CompletableFuture<Void> load(UUID uuid) {
        return repository.ensureAccount(uuid, startingBalance);
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID uuid) {
        return repository.getBalance(uuid);
    }

    @Override
    public CompletableFuture<Boolean> has(UUID uuid, BigDecimal amount) {
        BigDecimal needed = normalize(amount, false);
        return repository.getBalance(uuid).thenApply(balance -> balance.compareTo(needed) >= 0);
    }

    @Override
    public CompletableFuture<Void> deposit(UUID uuid, BigDecimal amount) {
        return repository.deposit(uuid, normalize(amount, true))
                .thenRun(() -> publisher.publish(uuid));
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID uuid, BigDecimal amount) {
        return repository.withdraw(uuid, normalize(amount, true))
                .thenApply(success -> {
                    if (success) {
                        publisher.publish(uuid);
                    }
                    return success;
                });
    }

    @Override
    public CompletableFuture<Boolean> transfer(UUID from, UUID to, BigDecimal amount) {
        if (from.equals(to)) {
            return CompletableFuture.completedFuture(false);
        }
        return repository.transfer(from, to, normalize(amount, true))
                .thenApply(success -> {
                    if (success) {
                        publisher.publish(from);
                        publisher.publish(to);
                    }
                    return success;
                });
    }

    @Override
    public CompletableFuture<Void> set(UUID uuid, BigDecimal amount) {
        return repository.set(uuid, normalize(amount, false))
                .thenRun(() -> publisher.publish(uuid));
    }

    /**
     * Rounds an amount to two decimal places (banker-free HALF_UP) and validates sign.
     *
     * @param amount        the raw amount
     * @param strictlyPositive if {@code true}, zero is rejected (for deposits/withdrawals/transfers);
     *                         if {@code false}, zero is allowed (for set/balance checks)
     * @return the normalised amount
     * @throws IllegalArgumentException if the amount is null or fails the sign check
     */
    private static BigDecimal normalize(BigDecimal amount, boolean strictlyPositive) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        if (strictlyPositive ? scaled.signum() <= 0 : scaled.signum() < 0) {
            throw new IllegalArgumentException("amount must be " + (strictlyPositive ? "positive" : "non-negative"));
        }
        return scaled;
    }
}
