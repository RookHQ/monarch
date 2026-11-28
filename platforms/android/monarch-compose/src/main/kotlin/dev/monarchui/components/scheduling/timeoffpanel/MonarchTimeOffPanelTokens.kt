package dev.monarchui.components.scheduling.timeoffpanel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Calendar
import java.util.TimeZone

/** Component tokens, colours and the pure logic of MonarchTimeOffPanel. */
internal object MonarchTimeOffPanelTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val RowGap: Dp = MonarchTokens.Space.space1.dp
    val PlaceholderHeight: Dp = MonarchTokens.Space.space12.dp
    const val PLACEHOLDERS: Int = 3
    const val DAY_MILLIS: Long = 86_400_000L
    const val MINUTE_MILLIS: Long = 60_000L
    const val DEFAULT_START_MINUTES: Int = 9 * 60
    const val DEFAULT_END_MINUTES: Int = 17 * 60

    /** The title and an entry's name. */
    fun text(colors: MonarchColors): Color = colors.textDefault

    /** The description, an entry's dates and a past entry's name. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The validation error in the sheet. */
    fun error(colors: MonarchColors): Color = colors.dangerStrong

    /** The date [instant] falls on in [zone], as its UTC midnight. */
    fun dayOf(instant: Long, zone: TimeZone): Long =
        Math.floorDiv(instant + zone.getOffset(instant), DAY_MILLIS) * DAY_MILLIS

    /** Whether [entry] has ended by [now]: a timed entry by its end instant, an all-day one by its last day. */
    fun isPast(entry: MonarchTimeOffEntry, now: Long, zone: TimeZone): Boolean =
        if (entry.allDay) entry.end < dayOf(now, zone) else entry.end < now

    /** The entries to list: all of them, or only those not past. */
    fun visible(entries: List<MonarchTimeOffEntry>, showPast: Boolean, now: Long, zone: TimeZone) =
        if (showPast) entries else entries.filterNot { isPast(it, now, zone) }

    /** The instant of [minutes] after midnight on [day] (a UTC midnight) as a wall time in [zone]. */
    fun wallToInstant(day: Long, minutes: Int, zone: TimeZone): Long {
        val date = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = day }
        return Calendar.getInstance(zone).apply {
            clear()
            set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
            add(Calendar.MINUTE, minutes)
        }.timeInMillis
    }

    /** Whether [entry] shows a remove button: removing is offered and the app allows it for this entry. */
    fun removable(
        entry: MonarchTimeOffEntry,
        offered: Boolean,
        canRemove: ((MonarchTimeOffEntry) -> Boolean)?,
    ): Boolean = offered && canRemove?.invoke(entry) != false

    /** Whether the end is before the start: the same day is allowed all day, a timed end must be later. */
    fun endBeforeStart(allDay: Boolean, start: Long, end: Long): Boolean = if (allDay) end < start else end <= start

    /** The draft from the form, or `null` when the end is before the start. */
    @Suppress("LongParameterList")
    fun draft(
        personId: String?,
        allDay: Boolean,
        start: Long,
        end: Long,
        type: String?,
        reason: String,
    ): MonarchTimeOffDraft? {
        if (endBeforeStart(allDay, start, end)) return null
        return MonarchTimeOffDraft(personId, allDay, start, end, type, reason.trim().ifEmpty { null })
    }
}
