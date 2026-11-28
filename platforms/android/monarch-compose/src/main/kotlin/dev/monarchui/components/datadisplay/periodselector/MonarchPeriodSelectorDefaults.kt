package dev.monarchui.components.datadisplay.periodselector

import dev.monarchui.internal.MonarchTimeZones
import java.util.Calendar
import java.util.Locale

/** Defaults and the date rules of `MonarchPeriodSelector`. */
public object MonarchPeriodSelectorDefaults {
    /** The presets offered by default, as on the web: today, 7 days, 30 days, this quarter and custom. */
    public val Presets: List<MonarchPeriodPreset> = listOf(
        MonarchPeriodPreset.Today,
        MonarchPeriodPreset.Last7Days,
        MonarchPeriodPreset.Last30Days,
        MonarchPeriodPreset.ThisQuarter,
        MonarchPeriodPreset.Custom,
    )

    /**
     * Today's date in [timeZone], as UTC midnight in epoch milliseconds, the
     * form the Material 3 date pickers and [resolve] use.
     *
     * @param nowMillis the current time.
     * @param timeZone the zone whose calendar day counts as today. An IANA id such as `"Europe/Berlin"`. `null`, or an
     * id the device does not know, is the device's zone.
     */
    public fun today(nowMillis: Long = System.currentTimeMillis(), timeZone: String? = null): Long =
        PeriodSelectorDays.dayOf(nowMillis, MonarchTimeZones.java(timeZone))

    /**
     * The first day of the week in [locale], as a `java.util.Calendar` day
     * constant (`Calendar.MONDAY`, `Calendar.SUNDAY`…).
     *
     * @param locale the locale whose week to use.
     */
    public fun firstDayOfWeek(locale: Locale): Int = Calendar.getInstance(locale).firstDayOfWeek

    /**
     * The days [value] covers, both ends included, relative to [today].
     * Returns `null` for a custom period with no range yet.
     *
     * @param value the period.
     * @param today today, UTC midnight in epoch milliseconds (see [today]).
     * @param firstDayOfWeek the first day of the week for [MonarchPeriodPreset.ThisWeek], a `java.util.Calendar`
     * day constant.
     */
    public fun resolve(
        value: MonarchPeriodValue,
        today: Long,
        firstDayOfWeek: Int = Calendar.MONDAY,
    ): MonarchPeriodRange? {
        val day = PeriodSelectorDays.startOfDay(today)
        Rolling[value.preset]?.let { count ->
            return MonarchPeriodRange(PeriodSelectorDays.addDays(day, -(count - 1)), day)
        }
        val days = PeriodSelectorDays
        return when (value.preset) {
            MonarchPeriodPreset.Today -> MonarchPeriodRange(day, day)
            MonarchPeriodPreset.Yesterday -> days.addDays(day, -1).let { MonarchPeriodRange(it, it) }
            MonarchPeriodPreset.ThisWeek -> MonarchPeriodRange(days.startOfWeek(day, firstDayOfWeek), day)
            MonarchPeriodPreset.ThisMonth -> MonarchPeriodRange(days.startOfMonth(day), day)
            MonarchPeriodPreset.LastMonth -> days.addMonths(days.startOfMonth(day), -1).let {
                MonarchPeriodRange(it, days.endOfMonth(it))
            }
            MonarchPeriodPreset.ThisQuarter -> MonarchPeriodRange(days.startOfQuarter(day), day)
            MonarchPeriodPreset.LastQuarter -> days.addMonths(days.startOfQuarter(day), -QUARTER_MONTHS).let {
                MonarchPeriodRange(it, days.endOfMonth(days.addMonths(it, QUARTER_MONTHS - 1)))
            }
            MonarchPeriodPreset.ThisYear -> MonarchPeriodRange(days.startOfYear(day), day)
            else -> value.range
        }
    }

    private const val QUARTER_MONTHS = 3

    /** The number of days of each rolling preset, today included. */
    private val Rolling: Map<MonarchPeriodPreset, Int> = mapOf(
        MonarchPeriodPreset.Last7Days to 7,
        MonarchPeriodPreset.Last14Days to 14,
        MonarchPeriodPreset.Last30Days to 30,
        MonarchPeriodPreset.Last90Days to 90,
        MonarchPeriodPreset.Last180Days to 180,
        MonarchPeriodPreset.Last365Days to 365,
    )
}
