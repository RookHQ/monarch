package dev.monarchui.components.technical.webhookdeliveries

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import android.icu.util.ULocale
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusRegistry
import dev.monarchui.internal.MonarchRelativeTime
import java.text.NumberFormat
import java.util.Locale

/** The numbers, times and counts MonarchWebhookDeliveries writes, in the given locale and time zone. */
internal object MonarchWebhookDeliveriesFormat {
    /** Formats a whole number for [locale]: "1,203". */
    fun number(value: Long, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)

    /** [date] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(date: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(date, now, locale)

    /** The time of a row: "Next attempt in 5 minutes" when one is scheduled, else when it was last attempted. */
    fun whenText(delivery: MonarchWebhookDelivery, now: Long, text: MonarchWebhookDeliveriesText): String {
        val next = MonarchWebhookDeliveriesTokens.nextAttempt(delivery)
        return if (next != null) text.nextAttempt(relativeTime(next, now)) else relativeTime(delivery.atMillis, now)
    }

    /** A date and time for the details sheet, in [locale] and [timeZone]: "Nov 14, 2026, 3:20 PM". */
    fun absoluteTime(millis: Long, locale: Locale, timeZone: java.util.TimeZone): String {
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, ULocale.forLocale(locale))
        format.timeZone = TimeZone.getTimeZone(timeZone.id)
        return format.format(java.util.Date(millis))
    }

    /** The counts line, in the order of [counts]: "12 delivered · 3 retrying". Empty without counts. */
    fun countLine(
        counts: Map<String, Int>,
        registry: MonarchStatusRegistry,
        text: MonarchWebhookDeliveriesText,
        locale: Locale,
    ): String = counts.entries.joinToString(MonarchWebhookDeliveriesTokens.SEPARATOR) { (key, count) ->
        text.count(
            number(count.toLong(), locale),
            MonarchWebhookDeliveriesTokens.statusName(key, registry).lowercase(locale),
        )
    }
}
