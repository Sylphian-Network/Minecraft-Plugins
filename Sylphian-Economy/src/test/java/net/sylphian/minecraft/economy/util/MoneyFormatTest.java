package net.sylphian.minecraft.economy.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyFormatTest {

    @BeforeAll
    static void useUsLocale() {
        // DecimalFormat uses the default locale; pin it so grouping/decimal separators are stable.
        Locale.setDefault(Locale.US);
    }

    @BeforeEach
    void resetSymbol() {
        MoneyFormat.configure("$");
    }

    @Test
    void formatGroupsThousandsAndRoundsHalfUp() {
        assertThat(MoneyFormat.format(new BigDecimal("1250"))).isEqualTo("$1,250.00");
        assertThat(MoneyFormat.format(new BigDecimal("2.005"))).isEqualTo("$2.01");
    }

    @Test
    void formatUsesConfiguredSymbol() {
        MoneyFormat.configure("€");
        assertThat(MoneyFormat.format(new BigDecimal("5"))).isEqualTo("€5.00");
    }

    @Test
    void parseAcceptsPlainAndFormattedInput() {
        assertThat(MoneyFormat.parse("1000")).isEqualByComparingTo("1000");
        assertThat(MoneyFormat.parse("12.50")).isEqualByComparingTo("12.50");
        assertThat(MoneyFormat.parse("1,000")).isEqualByComparingTo("1000");
        assertThat(MoneyFormat.parse("$50")).isEqualByComparingTo("50");
        assertThat(MoneyFormat.parse(".50")).isEqualByComparingTo("0.50");
    }

    @Test
    void parseReturnsNullForGarbage() {
        assertThat(MoneyFormat.parse("abc")).isNull();
        assertThat(MoneyFormat.parse("")).isNull();
        assertThat(MoneyFormat.parse("-")).isNull();
        assertThat(MoneyFormat.parse("12.5.5")).isNull();
        assertThat(MoneyFormat.parse("10 00")).isNull();
        assertThat(MoneyFormat.parse(null)).isNull();
    }

    @Test
    void parseRejectsScientificNotation() {
        assertThat(MoneyFormat.parse("1e9")).isNull();
        assertThat(MoneyFormat.parse("1E9")).isNull();
        assertThat(MoneyFormat.parse("1.5e2")).isNull();
    }

    @Test
    void parseAllowsNegativeForCallersToSignCheck() {
        assertThat(MoneyFormat.parse("-5")).isEqualByComparingTo("-5");
    }

    @Test
    void parseTreatsCommasAsGroupingOnly() {
        // Commas are stripped, never decimal separators: "1,0" is ten, not one.
        assertThat(MoneyFormat.parse("1,0")).isEqualByComparingTo("10");
    }
}
