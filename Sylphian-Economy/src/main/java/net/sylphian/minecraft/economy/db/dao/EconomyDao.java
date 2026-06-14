package net.sylphian.minecraft.economy.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * JDBI DAO for the mc_economy_balances table.
 *
 * <p>Credit/debit are relative updates ({@code balance = balance +/- :amount})
 * rather than read-modify-write, keeping them atomic at the row level.</p>
 */
public interface EconomyDao {

    /**
     * Creates an account row if one does not already exist. {@code INSERT IGNORE}
     * makes this safe to call repeatedly and from multiple servers concurrently.
     *
     * @param uuid    the player's UUID as a string
     * @param balance the starting balance for a freshly created row
     */
    @SqlUpdate("INSERT IGNORE INTO mc_economy_balances (uuid, balance) VALUES (:uuid, :balance)")
    void ensureAccount(@Bind("uuid") String uuid, @Bind("balance") BigDecimal balance);

    /**
     * Reads a balance.
     *
     * @param uuid the player's UUID as a string
     * @return the balance, or empty if no row exists
     */
    @SqlQuery("SELECT balance FROM mc_economy_balances WHERE uuid = :uuid")
    Optional<BigDecimal> findBalance(@Bind("uuid") String uuid);

    /**
     * Atomically adds to a balance.
     *
     * @param uuid   the player's UUID as a string
     * @param amount the amount to add
     * @return the number of rows affected (1 if the account existed)
     */
    @SqlUpdate("UPDATE mc_economy_balances SET balance = balance + :amount WHERE uuid = :uuid")
    int credit(@Bind("uuid") String uuid, @Bind("amount") BigDecimal amount);

    /**
     * Atomically subtracts from a balance only when sufficient funds exist. The
     * {@code AND balance >= :amount} guard means the row is left untouched (0 rows
     * affected) if the player cannot afford it — no separate balance check needed.
     *
     * @param uuid   the player's UUID as a string
     * @param amount the amount to remove
     * @return the number of rows affected (1 on success, 0 if funds were insufficient)
     */
    @SqlUpdate("UPDATE mc_economy_balances SET balance = balance - :amount WHERE uuid = :uuid AND balance >= :amount")
    int debitIfSufficient(@Bind("uuid") String uuid, @Bind("amount") BigDecimal amount);

    /**
     * Overwrites a balance with an exact value.
     *
     * @param uuid   the player's UUID as a string
     * @param amount the new balance
     * @return the number of rows affected
     */
    @SqlUpdate("UPDATE mc_economy_balances SET balance = :amount WHERE uuid = :uuid")
    int setBalance(@Bind("uuid") String uuid, @Bind("amount") BigDecimal amount);
}
