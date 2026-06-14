package net.sylphian.minecraft.economy.api;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The public, cross-plugin contract for the Sylphian economy, obtained via
 * {@link EconomyProvider}.
 */
public interface EconomyAPI {

    /**
     * Reads a player's balance.
     *
     * @param uuid the player's UUID
     * @return a future of the current balance (zero if the player has no account yet)
     */
    CompletableFuture<BigDecimal> getBalance(UUID uuid);

    /**
     * Checks whether a player can afford an amount.
     *
     * @param uuid   the player's UUID
     * @param amount the amount to test against
     * @return a future of {@code true} if the balance is greater than or equal to {@code amount}
     */
    CompletableFuture<Boolean> has(UUID uuid, BigDecimal amount);

    /**
     * Adds money to a player (e.g. a quest reward or crate payout).
     *
     * @param uuid   the player's UUID
     * @param amount a positive amount
     * @return a future that completes when applied
     */
    CompletableFuture<Void> deposit(UUID uuid, BigDecimal amount);

    /**
     * Removes money from a player if they can afford it (e.g. a shop purchase).
     *
     * @param uuid   the player's UUID
     * @param amount a positive amount
     * @return a future of {@code true} if charged, {@code false} if funds were insufficient
     */
    CompletableFuture<Boolean> withdraw(UUID uuid, BigDecimal amount);

    /**
     * Moves money from one player to another, all-or-nothing.
     *
     * @param from   the paying player's UUID
     * @param to     the receiving player's UUID
     * @param amount a positive amount
     * @return a future of {@code true} if transferred, {@code false} if the payer could not afford it
     */
    CompletableFuture<Boolean> transfer(UUID from, UUID to, BigDecimal amount);

    /**
     * Overwrites a player's balance (administrative use).
     *
     * @param uuid   the player's UUID
     * @param amount the new balance (non-negative)
     * @return a future that completes when set
     */
    CompletableFuture<Void> set(UUID uuid, BigDecimal amount);
}
