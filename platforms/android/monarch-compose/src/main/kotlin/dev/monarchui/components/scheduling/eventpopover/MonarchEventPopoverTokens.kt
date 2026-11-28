package dev.monarchui.components.scheduling.eventpopover

import android.icu.text.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The two lines of the time row: the day, then the times with the zone or "All day". */
internal data class EventPopoverWhen(val date: String, val time: String)

/** Component tokens and rules of MonarchEventPopover, built from primitives and roles only. */
internal object MonarchEventPopoverTokens {
    private const val DATE_SKELETON = "EEEMMMdy"
    private const val TIME_SKELETON = "jmm"
    private const val ZONE_SKELETON = "z"
    private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

    val SectionGap: Dp = MonarchTokens.Space.space3.dp
    val ItemGap: Dp = MonarchTokens.Space.space2.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val SwatchSize: Dp = MonarchTokens.Space.space3.dp

    /** The top offset of the swatch so it sits on the first line of the title. */
    val SwatchTop: Dp = MonarchTokens.Space.space1_5.dp
    val NotePadding: Dp = MonarchTokens.Space.space3.dp

    /** The swatch: the event's colour, or the accent role. */
    fun swatch(color: Color, colors: MonarchColors): Color =
        if (color == Color.Unspecified) colors.accentSolid else color

    /** The fill behind the note. */
    fun noteFill(colors: MonarchColors): Color = colors.bgSubtle

    /** The tone of the status badge, as on the web. */
    fun tone(status: MonarchEventChipStatus): MonarchTone = when (status) {
        MonarchEventChipStatus.Confirmed -> MonarchTone.Success
        MonarchEventChipStatus.Tentative -> MonarchTone.Warning
        MonarchEventChipStatus.Cancelled -> MonarchTone.Neutral
        MonarchEventChipStatus.Declined -> MonarchTone.Danger
    }

    /** The built-in name of a status. */
    fun statusRes(status: MonarchEventChipStatus): Int = when (status) {
        MonarchEventChipStatus.Confirmed -> R.string.monarch_eventpopover_status_confirmed
        MonarchEventChipStatus.Tentative -> R.string.monarch_eventpopover_status_tentative
        MonarchEventChipStatus.Cancelled -> R.string.monarch_eventpopover_status_cancelled
        MonarchEventChipStatus.Declined -> R.string.monarch_eventpopover_status_declined
    }

    /** Cancelled and declined titles are struck through. */
    fun titleDecoration(status: MonarchEventChipStatus?): TextDecoration? = when (status) {
        MonarchEventChipStatus.Cancelled, MonarchEventChipStatus.Declined -> TextDecoration.LineThrough
        else -> null
    }

    /** The end, or `null` when there is none or it is before the start. */
    fun validEnd(start: Long, end: Long?): Long? = end?.takeIf { it >= start }

    /**
     * The time row, as on the web. An all-day event shows its day, or "first
     * – last", in UTC (days are UTC midnights, as in the Material 3 date
     * picker) and [allDay]. A timed event shows the day and "9:30 AM – 10:15
     * AM EST" in [zone]; one that spans days shows the start day and time on
     * the first line and the end day, time and zone on the second.
     */
    @Suppress("LongParameterList")
    fun formatWhen(
        start: Long,
        end: Long?,
        allDay: Boolean,
        zone: TimeZone,
        locale: Locale,
        allDayLabel: String,
        range: (String, String) -> String,
    ): EventPopoverWhen {
        val last = validEnd(start, end)
        if (allDay) {
            val day = formatter(DATE_SKELETON, locale, Utc)
            val first = day.format(Date(start))
            val lastDay = last?.let { day.format(Date(it)) } ?: first
            return EventPopoverWhen(if (lastDay == first) first else range(first, lastDay), allDayLabel)
        }
        val day = formatter(DATE_SKELETON, locale, zone)
        val time = formatter(TIME_SKELETON, locale, zone)
        val zoneName = formatter(ZONE_SKELETON, locale, zone).format(Date(start))
        val date = day.format(Date(start))
        val startTime = time.format(Date(start))
        if (last == null) return EventPopoverWhen(date, "$startTime $zoneName".trim())
        val endDate = day.format(Date(last))
        val endTime = time.format(Date(last))
        return if (endDate == date) {
            EventPopoverWhen(date, "${range(startTime, endTime)} $zoneName".trim())
        } else {
            EventPopoverWhen(range("$date $startTime", "").trimEnd(), "$endDate $endTime $zoneName".trim())
        }
    }

    private fun formatter(skeleton: String, locale: Locale, zone: TimeZone): DateFormat =
        DateFormat.getInstanceForSkeleton(skeleton, locale).apply {
            timeZone = android.icu.util.TimeZone.getTimeZone(zone.id)
        }
}
