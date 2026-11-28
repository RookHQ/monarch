package com.rookhq.monarch.components.intelligence.aitraceview

import android.content.res.Resources
import com.rookhq.monarch.R
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** The number, duration and money formats of MonarchAiTraceView, as the web writes them. */
internal object MonarchAiTraceViewFormat {
    private const val SECOND = 1_000.0
    private const val MINUTE_SECONDS = 60L
    private const val MILLIS_LIMIT = 999.5
    private const val TWO_DIGIT_LIMIT = 9.995
    private const val ONE_DIGIT_LIMIT = 59.95
    private const val SMALL_MILLIS = 10.0
    private const val MICROS = 1_000_000.0
    private const val SMALL_COST_DIGITS = 4
    private const val COST_DIGITS = 2

    private fun number(locale: Locale, digits: Int): NumberFormat =
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = digits
            minimumFractionDigits = 0
        }

    /** A duration in milliseconds: "850 ms", "1.42 s", "12.5 s", "2 min 5 s", "3 min". */
    fun duration(millis: Long, locale: Locale, resources: Resources): String {
        val value = millis.coerceAtLeast(0L).toDouble()
        val seconds = value / SECOND
        return when {
            value < MILLIS_LIMIT -> {
                val digits = if (value > 0 && value < SMALL_MILLIS) 1 else 0
                resources.getString(R.string.monarch_aitraceview_milliseconds, number(locale, digits).format(value))
            }
            seconds < TWO_DIGIT_LIMIT ->
                resources.getString(R.string.monarch_aitraceview_seconds, number(locale, 2).format(seconds))
            seconds < ONE_DIGIT_LIMIT ->
                resources.getString(R.string.monarch_aitraceview_seconds, number(locale, 1).format(seconds))
            else -> minutes(seconds.roundToLong(), locale, resources)
        }
    }

    private fun minutes(total: Long, locale: Locale, resources: Resources): String {
        val whole = number(locale, 0)
        val minutes = whole.format(total / MINUTE_SECONDS)
        val rest = total % MINUTE_SECONDS
        return if (rest == 0L) {
            resources.getString(R.string.monarch_aitraceview_minutes, minutes)
        } else {
            resources.getString(R.string.monarch_aitraceview_minutes_seconds, minutes, whole.format(rest))
        }
    }

    /** A count in [locale]. */
    fun count(value: Long, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)

    /** Money from micros: two decimals, up to four below one unit; the number and the code for an unknown code. */
    fun cost(micros: Long, currency: String, locale: Locale): String {
        val amount = micros / MICROS
        val small = amount != 0.0 && abs(amount) < 1
        return try {
            NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = Currency.getInstance(currency)
                minimumFractionDigits = COST_DIGITS
                maximumFractionDigits = if (small) SMALL_COST_DIGITS else COST_DIGITS
            }.format(amount)
        } catch (_: IllegalArgumentException) {
            "${number(locale, SMALL_COST_DIGITS).format(amount)} $currency"
        }
    }
}
