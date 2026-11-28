package dev.monarchui.internal

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * The value and percentage writers of the charts, in the user's locale:
 * "1,234.5" (up to three decimals, the locale default), "1.2K" on a compact
 * axis, and "12.5%" for a share from 0 to 1. One place, so the line, area,
 * burndown, bar, donut and proportion charts write numbers alike.
 */
internal object MonarchChartFormats {
    /** A writer of chart values in [locale]: compact ("1.2K") when [compact], else the locale's plain numbers. */
    fun value(locale: Locale, compact: Boolean = false): (Double) -> String = if (compact) {
        val writer = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
        val format: (Double) -> String = { writer.format(it) }
        format
    } else {
        val writer = NumberFormat.getInstance(locale)
        val format: (Double) -> String = { writer.format(it) }
        format
    }

    /** A writer of shares from 0 to 1 as percentages in [locale], with up to [fractionDigits] decimals. */
    fun percent(locale: Locale, fractionDigits: Int): (Double) -> String {
        val writer = NumberFormat.getPercentInstance(locale).apply {
            maximumFractionDigits = fractionDigits.coerceAtLeast(0)
            minimumFractionDigits = 0
        }
        return { writer.format(it) }
    }
}

/** [formatValue], or [MonarchChartFormats.value] in the current locale, kept across recompositions. */
@Composable
internal fun rememberMonarchChartValueFormat(
    formatValue: ((Double) -> String)?,
    compact: Boolean = false,
): (Double) -> String {
    val locale = LocalConfiguration.current.locales[0]
    return formatValue ?: remember(locale, compact) { MonarchChartFormats.value(locale, compact) }
}

/** [MonarchChartFormats.percent] in the current locale, kept across recompositions. */
@Composable
internal fun rememberMonarchChartPercentFormat(fractionDigits: Int): (Double) -> String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, fractionDigits) { MonarchChartFormats.percent(locale, fractionDigits) }
}
