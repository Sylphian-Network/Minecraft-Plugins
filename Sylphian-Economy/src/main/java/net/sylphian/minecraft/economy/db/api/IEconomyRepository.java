package net.sylphian.minecraft.economy.db.api;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for economy balance persistence operations.
 *
 * <p>All methods return a {@link CompletableFuture} so database I/O never blocks
 * the main server thread. Mutating operations are atomic; compound operations
 * ({@link #transfer}) run in a single transaction (all-or-nothing).</p>
 */
public interface IEconomyRepository {

    /**
     * Reads a player's current balance.
     *
     * @param uuid the player's UUID
     * @return a future of the balance, or {@link BigDecimal#ZERO} if no account row exists yet
     */
    CompletableFuture<BigDecimal> getBalance(UUID uuid);

    /**
     * Ensures an account row exists for the player, creating it with the given
     * starting balance if absent. A no-op if the row already exists.
     *
     * @param uuid     the player's UUID
     * @param starting the starting balance to seed a new account with
     * @return a future that completes when the row is guaranteed to exist
     */
    CompletableFuture<Void> ensureAccount(UUID uuid, BigDecimal starting);

    /**
     * Atomically adds an amount to a player's balance, creating the account if needed.
     *
     * @param uuid   the player's UUID
     * @param amount the (positive) amount to add
     * @return a future that completes when the credit is applied
     */
    CompletableFuture<Void> deposit(UUID uuid, BigDecimal amount);

    /**
     * Atomically subtracts an amount from a player's balance, but only if the
     * balance is sufficient. The check and the debit happen in a single SQL
     * statement, so two concurrent withdrawals cannot both succeed and overdraw.
     *
     * @param uuid   the player's UUID
     * @param amount the (positive) amount to remove
     * @return a future of {@code true} if the debit succeeded, {@code false} if funds were insufficient
     */
    CompletableFuture<Boolean> withdraw(UUID uuid, BigDecimal amount);

    /**
     * Overwrites a player's balance with an exact value (administrative use).
     *
     * @param uuid   the player's UUID
     * @param amount the new balance
     * @return a future that completes when the balance is set
     */
    CompletableFuture<Void> set(UUID uuid, BigDecimal amount);

    /**
     * Atomically moves an amount from one player to another inside a single
     * transaction. Either both sides apply or neither does.
     *
     * @param from   the paying player's UUID
     * @param to     the receiving player's UUID
     * @param amount the (positive) amount to move
     * @return a future of {@code true} if the transfer succeeded, {@code false} if the payer had insufficient funds
     */
    CompletableFuture<Boolean> transfer(UUID from, UUID to, BigDecimal amount);
}
