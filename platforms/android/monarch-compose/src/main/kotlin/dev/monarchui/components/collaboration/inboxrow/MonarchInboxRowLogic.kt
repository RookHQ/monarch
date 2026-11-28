package dev.monarchui.components.collaboration.inboxrow

import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import dev.monarchui.internal.currentIcuTimeZone
import java.util.Locale

/** A compact age: a unit and a number, for example minutes and 5. */
internal data class MonarchInboxRowAge(val unit: MonarchInboxRowAgeUnit, val value: Int)

/** The rules of MonarchInboxRow: the unread state, the age and the swipe directions. */
internal object MonarchInboxRowLogic {
    /** The largest count written as a number; above it "99+". */
    const val MAX_COUNT: Int = 99

    private const val MINUTE: Long = 60_000L
    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_DAY = 24
    private const val DAYS_PER_WEEK = 7
    private const val DAYS_FOR_DATE = 30

    /** Whether the row is unread: marked unread, or with a count above zero. */
    fun isUnread(unread: Boolean, unreadCount: Int?): Boolean = unread || (unreadCount ?: 0) > 0

    /** The visible count: the number, or "99+" above 99. */
    fun countText(count: Int): String = if (count > MAX_COUNT) "$MAX_COUNT+" else count.toString()

    /**
     * The compact age of [timestamp] at [now], as on the web: "now" under a
     * minute, then minutes, hours, days and weeks. `null` from 30 days, when a
     * short date is shown instead. A time in the future reads as now.
     */
    fun compactAge(timestamp: Long, now: Long): MonarchInboxRowAge? {
        val minutes = ((now - timestamp).coerceAtLeast(0L) / MINUTE).toInt()
        val hours = minutes / MINUTES_PER_HOUR
        val days = hours / HOURS_PER_DAY
        return when {
            minutes < 1 -> MonarchInboxRowAge(MonarchInboxRowAgeUnit.Now, 0)
            minutes < MINUTES_PER_HOUR -> MonarchInboxRowAge(MonarchInboxRowAgeUnit.Minutes, minutes)
            hours < HOURS_PER_DAY -> MonarchInboxRowAge(MonarchInboxRowAgeUnit.Hours, hours)
            days < DAYS_PER_WEEK -> MonarchInboxRowAge(MonarchInboxRowAgeUnit.Days, days)
            days < DAYS_FOR_DATE -> MonarchInboxRowAge(MonarchInboxRowAgeUnit.Weeks, days / DAYS_PER_WEEK)
            else -> null
        }
    }

    /** "12 Mar", or "12 Mar 2025" when [timestamp] is not in the year of [now], in [timeZone]. */
    fun shortDate(timestamp: Long, now: Long, locale: Locale, timeZone: TimeZone = currentIcuTimeZone()): String {
        val skeleton = if (year(timestamp, timeZone) == year(now, timeZone)) "dMMM" else "dMMMy"
        val format = DateFormat.getInstanceForSkeleton(skeleton, locale)
        format.timeZone = timeZone
        return format.format(timestamp)
    }

    /** The full date and time TalkBack reads instead of the compact age. */
    fun fullDate(timestamp: Long, locale: Locale, timeZone: TimeZone = currentIcuTimeZone()): String {
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        format.timeZone = timeZone
        return format.format(timestamp)
    }

    /** The action run by swiping toward the end. */
    fun towardEnd(actions: List<MonarchInboxRowSwipeAction>): MonarchInboxRowSwipeAction? =
        actions.firstOrNull { it is MonarchInboxRowSwipeAction.ToggleRead }

    /** The action run by swiping toward the start: archive, or delete when there is no archive. */
    fun towardStart(actions: List<MonarchInboxRowSwipeAction>): MonarchInboxRowSwipeAction? =
        actions.firstOrNull { it is MonarchInboxRowSwipeAction.Archive }
            ?: actions.firstOrNull { it is MonarchInboxRowSwipeAction.Delete }

    private fun year(millis: Long, timeZone: TimeZone): Int {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = millis
        return calendar.get(Calendar.YEAR)
    }
}
