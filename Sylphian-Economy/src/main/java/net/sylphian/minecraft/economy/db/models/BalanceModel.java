package net.sylphian.minecraft.economy.db.models;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Database model representing a record in the mc_economy_balances table.
 *
 * @param uuid    the player's Mojang UUID (primary key)
 * @param balance the player's current balance, with exact decimal precision
 */
public record BalanceModel(
        UUID uuid,
        BigDecimal balance
) {}
