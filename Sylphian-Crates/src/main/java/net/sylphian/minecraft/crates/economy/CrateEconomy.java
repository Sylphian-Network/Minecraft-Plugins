package net.sylphian.minecraft.crates.economy;

import net.sylphian.minecraft.economy.api.EconomyProvider;
import net.sylphian.minecraft.economy.util.MoneyFormat;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Soft-dependency bridge to Sylphian-Economy.
 */
public final class CrateEconomy {

    private CrateEconomy() {
    }

    /**
     * Deposits an amount into a player's balance, if an economy is registered.
     *
     * @param uuid   the player's UUID
     * @param amount the amount to deposit
     */
    public static void deposit(UUID uuid, BigDecimal amount) {
        if (EconomyProvider.isAvailable()) {
            EconomyProvider.get().deposit(uuid, amount);
        }
    }

    /**
     * @return the economy's configured currency symbol
     */
    public static String currencySymbol() {
        return MoneyFormat.symbol();
    }
}
