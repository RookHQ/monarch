package com.rookhq.monarch.components.charts.deltaindicator

import com.rookhq.monarch.foundation.MonarchTone
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * What a change works out to: its direction, its tone and its size as text.
 *
 * @property direction `null` when there is nothing to compare with.
 * @property tone Success for a change in the good direction, Danger for the other one, Neutral otherwise.
 * @property magnitude the size of the change without a sign, such as "12%". Empty when flat or without data.
 */
internal data class MonarchDeltaIndicatorReading(
    val direction: MonarchDeltaIndicatorDirection?,
    val tone: MonarchTone,
    val magnitude: String,
) {
    /** The visible change: "+12%" or "−3%" with a real minus sign (U+2212). Empty when flat or without data. */
    val signedText: String
        get() = when (direction) {
            MonarchDeltaIndicatorDirection.Up -> "+$magnitude"
            MonarchDeltaIndicatorDirection.Down -> "−$magnitude"
            MonarchDeltaIndicatorDirection.Flat, null -> ""
        }

    companion object {
        private const val PERCENT_FRACTION_DIGITS = 1

        /** The change from [previous] to [current] as a ratio of the absolute previous value, or `null`. */
        fun change(current: Double?, previous: Double?): Double? {
            if (current == null || previous == null || previous == 0.0) return null
            return ((current - previous) / abs(previous)).takeIf { it.isFinite() }
        }

        /** The default writer of a change: a percentage with at most one fraction digit in [locale]. */
        fun percent(locale: Locale): (Double) -> String {
            val format = NumberFormat.getPercentInstance(locale).apply {
                maximumFractionDigits = PERCENT_FRACTION_DIGITS
            }
            return { value -> format.format(value) }
        }

        /**
         * Reads [value]. A change whose size is at or below [flatThreshold], or
         * that [write] writes the same as zero, is flat, so the indicator never
         * shows "+0%".
         */
        fun of(
            value: Double?,
            goodDirection: MonarchDeltaIndicatorGoodDirection,
            flatThreshold: Double,
            write: (Double) -> String,
        ): MonarchDeltaIndicatorReading {
            if (value == null || !value.isFinite()) {
                return MonarchDeltaIndicatorReading(null, MonarchTone.Neutral, "")
            }
            val size = abs(value)
            if (size <= flatThreshold || write(size) == write(0.0)) {
                return MonarchDeltaIndicatorReading(MonarchDeltaIndicatorDirection.Flat, MonarchTone.Neutral, "")
            }
            val direction = if (value > 0) MonarchDeltaIndicatorDirection.Up else MonarchDeltaIndicatorDirection.Down
            val better = when (goodDirection) {
                MonarchDeltaIndicatorGoodDirection.Up -> direction == MonarchDeltaIndicatorDirection.Up
                MonarchDeltaIndicatorGoodDirection.Down -> direction == MonarchDeltaIndicatorDirection.Down
                MonarchDeltaIndicatorGoodDirection.Neither -> null
            }
            val tone = when (better) {
                true -> MonarchTone.Success
                false -> MonarchTone.Danger
                null -> MonarchTone.Neutral
            }
            return MonarchDeltaIndicatorReading(direction, tone, write(size))
        }
    }
}
