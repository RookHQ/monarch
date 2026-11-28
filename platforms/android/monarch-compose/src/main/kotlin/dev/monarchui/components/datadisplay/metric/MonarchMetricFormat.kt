package dev.monarchui.components.datadisplay.metric

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import dev.monarchui.components.billing.money.MonarchMoneyFormat
import dev.monarchui.components.billing.money.MonarchMoneyNotation
import java.math.BigDecimal
import java.util.Locale

/**
 * How a `MonarchMetric` writes its number, with the platform's
 * `android.icu.text.NumberFormat`. The same three forms exist on every
 * platform.
 */
public sealed class MonarchMetricFormat {
    /**
     * Writes [value] the way `MonarchMetric` shows it, for tables that need the same text.
     *
     * @param value the number. It must be finite.
     * @param compact writes the short form of the locale: 12K, €3.4M.
     * @param locale the locale to write in.
     * @return the formatted number.
     */
    public abstract fun format(value: Double, compact: Boolean, locale: Locale): String

    /** A plain number with the locale's grouping: 12,345. The default. */
    public data object Number : MonarchMetricFormat() {
        override fun format(value: Double, compact: Boolean, locale: Locale): String =
            if (compact) {
                CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT).format(value)
            } else {
                NumberFormat.getNumberInstance(locale).format(value)
            }
    }

    /** A ratio written as a percentage with at most one decimal, as the web: 0.12 is 12%, 0.081 is 8.1%. */
    public data object Percent : MonarchMetricFormat() {
        override fun format(value: Double, compact: Boolean, locale: Locale): String =
            NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 1 }.format(value)
    }

    /**
     * An amount of money, written by `MonarchMoneyFormat`.
     *
     * @property code the ISO 4217 code, such as EUR.
     */
    public data class Currency(val code: String) : MonarchMetricFormat() {
        override fun format(value: Double, compact: Boolean, locale: Locale): String =
            MonarchMoneyFormat.format(
                amount = BigDecimal.valueOf(value),
                currencyCode = code,
                locale = locale,
                notation = if (compact) MonarchMoneyNotation.Compact else MonarchMoneyNotation.Standard,
            )
    }
}
