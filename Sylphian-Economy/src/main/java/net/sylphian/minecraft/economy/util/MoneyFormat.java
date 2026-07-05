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
     * Parses a user-entered amount string into a BigDecimal. Accepts plain decimal
     * notation, commas as grouping, and the configured symbol; rejects scientific
     * notation.
     *
     * @param input the raw input (e.g. {@code "100"}, {@code "1,250.50"} or {@code "$5"})
     * @return the parsed amount, or {@code null} if the input is not a valid plain number
     */
    public static BigDecimal parse(String input) {
        if (input == null) {
            return null;
        }
        String cleaned = input.replace(",", "").replace(symbol, "").trim();
        if (!cleaned.matches("-?(\\d+(\\.\\d+)?|\\.\\d+)")) {
            return null;
        }
        return new BigDecimal(cleaned);
    }
}
