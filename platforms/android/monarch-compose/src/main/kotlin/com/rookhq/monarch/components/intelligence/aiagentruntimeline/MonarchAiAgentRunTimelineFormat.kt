package com.rookhq.monarch.components.intelligence.aiagentruntimeline

import android.content.res.Resources
import com.rookhq.monarch.R
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.roundToLong

/** The number, duration and money formats of MonarchAiAgentRunTimeline. */
internal object MonarchAiAgentRunTimelineFormat {
    private const val SECOND = 1_000L
    private const val TEN_SECONDS = 10_000L
    private const val MINUTE = 60_000L
    private const val MICROS = 1_000_000.0
    private const val SMALL_COST_DIGITS = 4

    /** A duration as the web writes it: "850 ms", "4.2 s", "12 s", "2 min 5 s", "3 min". */
    fun formatDuration(millis: Long, resources: Resources): String {
        val value = millis.coerceAtLeast(0L)
        return when {
            value < SECOND -> resources.getString(R.string.monarch_aiagentruntimeline_milliseconds, value.toString())
            value < MINUTE -> {
                val format = NumberFormat.getNumberInstance().apply {
                    val digits = if (value < TEN_SECONDS) 1 else 0
                    minimumFractionDigits = digits
                    maximumFractionDigits = digits
                }
                val seconds = format.format(value / SECOND.toDouble())
                resources.getString(R.string.monarch_aiagentruntimeline_seconds, seconds)
            }
            else -> {
                val minutes = value / MINUTE
                val seconds = ((value % MINUTE) / SECOND.toDouble()).roundToLong()
                if (seconds == 0L) {
                    resources.getString(R.string.monarch_aiagentruntimeline_minutes, minutes)
                } else {
                    resources.getString(R.string.monarch_aiagentruntimeline_minutes_seconds, minutes, seconds)
                }
            }
        }
    }

    /** A count of tokens in the person's locale. */
    fun formatCount(count: Long): String = NumberFormat.getIntegerInstance().format(count)

    /** A cost in micros in [currency], with up to four decimals below one unit; number and code for an unknown code. */
    fun formatCost(micros: Long, currency: String): String {
        val amount = micros / MICROS
        return try {
            NumberFormat.getCurrencyInstance().apply {
                this.currency = Currency.getInstance(currency)
                // Model calls often cost fractions of a cent; keep up to four digits below one unit.
                if (amount < 1) maximumFractionDigits = maxOf(maximumFractionDigits, SMALL_COST_DIGITS)
            }.format(amount)
        } catch (_: IllegalArgumentException) {
            "${NumberFormat.getNumberInstance().format(amount)} $currency"
        }
    }
}
