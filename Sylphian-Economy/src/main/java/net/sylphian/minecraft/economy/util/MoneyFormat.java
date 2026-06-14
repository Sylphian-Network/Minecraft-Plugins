package net.sylphian.minecraft.economy.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/** Renders and parses currency for display and commands. The symbol is configurable. */
public final class MoneyFormat {

    private static volatile String symbol = "$";

    private static final ThreadLocal<DecimalFormat> DISPLAY = ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.00"));

    private MoneyFormat() {
    }

    /**
     * Sets the currency symbol shown to players. Called once on enable from config.
     *
     * @param currencySymbol the symbol prefixed to formatted amounts
     */
    public static void configure(String currencySymbol) {
        symbol = currencySymbol;
    }

    /** @return the configured currency symbol */
    public static String symbol() {
        return symbol;
    }

    /**
     * Formats an amount with the configured currency symbol and thousands grouping
     * (e.g. {@code 1,250.00}).
     *
     * @param amount the amount to format
     * @return the formatted string
     */
    public static String format(BigDecimal amount) {
        return symbol + DISPLAY.get().format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Parses a user-entered amount string into a BigDecimal.
     *
     * @param input the raw input (e.g. {@code "100"} or {@code "12.50"})
     * @return the parsed amount, or {@code null} if the input is not a valid number
     */
    public static BigDecimal parse(String input) {
        try {
            return new BigDecimal(input.replace(",", "").replace(symbol, "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
