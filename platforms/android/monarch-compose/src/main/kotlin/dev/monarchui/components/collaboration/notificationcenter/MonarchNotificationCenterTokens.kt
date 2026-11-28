package dev.monarchui.components.collaboration.notificationcenter

import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** A day of notifications: its day number and its items, newest first. */
internal data class NotificationCenterDay(val day: Long, val items: List<MonarchNotificationCenterItem>)

/** What a day heading says. */
internal enum class NotificationCenterDayName { Today, Yesterday, Date }

/** Component tokens and pure logic of MonarchNotificationCenter, built from primitives and roles only. */
internal object MonarchNotificationCenterTokens {
    val HeaderPadding: PaddingValues = PaddingValues(
        start = MonarchTokens.Space.space4.dp,
        end = MonarchTokens.Space.space2.dp,
        top = MonarchTokens.Space.space2.dp,
        bottom = MonarchTokens.Space.space2.dp,
    )
    val HeadingPadding: PaddingValues = PaddingValues(
        start = MonarchTokens.Space.space4.dp,
        end = MonarchTokens.Space.space4.dp,
        top = MonarchTokens.Space.space3.dp,
        bottom = MonarchTokens.Space.space1.dp,
    )
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val StatePadding: Dp = MonarchTokens.Space.space4.dp

    /** Rows shown while the first page loads. */
    const val PLACEHOLDER_ROWS: Int = 3

    /** The largest count written as a number; above it "99+". */
    const val MAX_COUNT: Int = 99

    private const val DAY_MILLIS: Long = 86_400_000L

    /** The unread total: the server's [unreadCount] when given, else the unread loaded items. Never below zero. */
    fun unreadTotal(items: List<MonarchNotificationCenterItem>, unreadCount: Int?): Int =
        (unreadCount ?: items.count { !it.read }).coerceAtLeast(0)

    /** The visible count: the number, or "99+" above 99. */
    fun countText(count: Int): String = if (count > MAX_COUNT) "$MAX_COUNT+" else count.toString()

    /** The items the tab shows. */
    fun visible(
        items: List<MonarchNotificationCenterItem>,
        filter: MonarchNotificationCenterFilter,
    ): List<MonarchNotificationCenterItem> = when (filter) {
        MonarchNotificationCenterFilter.All -> items
        MonarchNotificationCenterFilter.Unread -> items.filter { !it.read }
    }

    /** The day number of [millis] in [timeZone]: days since 1970-01-01 there. */
    fun day(millis: Long, timeZone: TimeZone): Long = Math.floorDiv(millis + timeZone.getOffset(millis), DAY_MILLIS)

    /** [items] in runs of the same day, in their order. A day that appears again later starts a new run. */
    fun groupByDay(items: List<MonarchNotificationCenterItem>, timeZone: TimeZone): List<NotificationCenterDay> {
        val days = mutableListOf<NotificationCenterDay>()
        items.forEach { item ->
            val day = day(item.time, timeZone)
            val last = days.lastOrNull()
            if (last != null && last.day == day) {
                days[days.lastIndex] = last.copy(items = last.items + item)
            } else {
                days += NotificationCenterDay(day, listOf(item))
            }
        }
        return days
    }

    /** Whether [day] is today, yesterday or another date, at [now] in [timeZone]. */
    fun dayName(day: Long, now: Long, timeZone: TimeZone): NotificationCenterDayName =
        when (day(now, timeZone) - day) {
            0L -> NotificationCenterDayName.Today
            1L -> NotificationCenterDayName.Yesterday
            else -> NotificationCenterDayName.Date
        }

    /** The heading of an older day: "Monday, March 9", with the year when it is not the year of [now]. */
    fun dateHeading(time: Long, now: Long, locale: Locale, timeZone: TimeZone): String {
        val skeleton = if (year(time, timeZone) == year(now, timeZone)) "EEEEMMMMd" else "EEEEyMMMMd"
        val format = DateFormat.getInstanceForSkeleton(skeleton, locale)
        format.timeZone = timeZone
        return format.format(time)
    }

    private fun year(millis: Long, timeZone: TimeZone): Int {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = millis
        return calendar.get(Calendar.YEAR)
    }
}
