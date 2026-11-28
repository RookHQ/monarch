package com.rookhq.monarch.internal

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import java.util.Locale

/**
 * Numbers written in the user's locale ("1,234.5", "1.234,5", "١٬٢٣٤٫٥") and
 * the dash shown for a missing value. Chart, table and summary components
 * use these instead of their own `NumberFormat` set-ups.
 */
internal object MonarchNumberText {
    /** The mark shown in place of a missing value. TalkBack reads a word instead, see [MonarchEmptyValueText]. */
    const val EMPTY_MARK: String = "—"

    private const val DEFAULT_FRACTION_DIGITS = 2
    private const val PERCENT_FRACTION_DIGITS = 1

    /** A writer of plain numbers in [locale], with grouping and up to [maxFractionDigits] digits. */
    fun number(locale: Locale, maxFractionDigits: Int = DEFAULT_FRACTION_DIGITS): (Double) -> String {
        val format = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = maxFractionDigits }
        return { value -> format.format(value) }
    }

    /** A writer of ratios as percentages in [locale]: 0.125 is "12.5%". */
    fun percent(locale: Locale, maxFractionDigits: Int = PERCENT_FRACTION_DIGITS): (Double) -> String {
        val format = NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = maxFractionDigits }
        return { ratio -> format.format(ratio) }
    }

    /** A writer of large numbers in the short form of [locale]: 1,200 is "1.2K". */
    fun compact(locale: Locale): (Double) -> String {
        val format = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
        return { value -> upperScale(format.format(value)) }
    }

    /**
     * [text] with a lower-case English scale letter written as a capital: "4.2k" is "4.2K", "1m" is "1M". Some
     * English locale data (en-GB) writes the short scales in lower case; the web and every platform write capitals.
     */
    fun upperScale(text: String): String =
        if (SCALE_SUFFIX.matches(text)) text.dropLast(1) + text.last().uppercaseChar() else text

    private val SCALE_SUFFIX = Regex("[^a-z]*[0-9][0-9.,\\u00a0\\u202f ]*[kmbt]")

    /** [value] written by [write], or the [EMPTY_MARK] when there is none. */
    fun orEmpty(value: Double?, write: (Double) -> String): String = value?.let(write) ?: EMPTY_MARK
}

/**
 * The dash of a missing value, read by TalkBack as [spokenLabel] ("Not set",
 * "No value"). The word is the node's text, not a content description, so a
 * row that merges its children reads "Label, Not set"; a content description
 * on a cleared node replaces the label instead.
 */
@Composable
internal fun MonarchEmptyValueText(spokenLabel: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = MonarchNumberText.EMPTY_MARK,
        modifier = modifier.clearAndSetSemantics { text = AnnotatedString(spokenLabel) },
        style = style,
        color = color,
    )
}
