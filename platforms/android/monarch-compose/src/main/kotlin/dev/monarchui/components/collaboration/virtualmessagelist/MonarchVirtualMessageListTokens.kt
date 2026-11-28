package dev.monarchui.components.collaboration.virtualmessagelist

import android.icu.text.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Component tokens and pure logic of MonarchVirtualMessageList, built from primitives and roles only. */
internal object MonarchVirtualMessageListTokens {
    val SeparatorPadding: Dp = MonarchTokens.Space.space2.dp
    val SeparatorGap: Dp = MonarchTokens.Space.space3.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val JumpPadding: Dp = MonarchTokens.Space.space3.dp

    /** How far from the bottom still counts as at the bottom, as on the web. */
    val AtBottomThreshold: Dp = MonarchTokens.Space.space20.dp

    /** Consecutive messages by one author within five minutes are grouped, as on the web. */
    const val GROUP_WINDOW_MILLIS: Long = 300_000L

    /** Older messages are asked for when the top is this many rows away. */
    const val LOAD_OLDER_THRESHOLD: Int = 5

    /** Rows of the first-load placeholder. */
    const val PLACEHOLDER_ROWS: Int = 4

    /** One row of the lazy list, newest first. */
    sealed interface Entry {
        /** The lazy list key: stable across loads. */
        val key: Any

        /** A message: its index in the items, oldest first, and whether it continues a group. */
        data class Message(val index: Int, val grouped: Boolean, override val key: Any) : Entry

        /** The separator above the first message of a day; [dayStart] is local midnight. */
        data class Day(val dayStart: Long) : Entry {
            override val key: Any get() = "$DAY_KEY_PREFIX$dayStart"
        }

        /** The "New messages" divider above the first unread message. */
        data object Unread : Entry {
            override val key: Any get() = EntryKey.Unread
        }

        /** Load older, its progress or its failure, at the top. */
        data object Older : Entry {
            override val key: Any get() = EntryKey.Older
        }

        /** The app's header above the first message, once all history is loaded. */
        data object Header : Entry {
            override val key: Any get() = EntryKey.Header
        }

        /** The app's footer below the last message. */
        data object Footer : Entry {
            override val key: Any get() = EntryKey.Footer
        }
    }

    /** Keys of the rows that are not messages: an enum, so it can be saved in a Bundle and no app key equals it. */
    enum class EntryKey { Unread, Older, Header, Footer }

    private const val DAY_KEY_PREFIX = "monarch-virtualmessagelist-day-"

    /**
     * The rows of the list, newest first for a `reverseLayout` list: the
     * footer, then each message preceded (above it) by the "New messages"
     * divider at [firstUnreadIndex] and by a day separator at each new day,
     * then the older-messages row while there is older history, or the header
     * once there is none.
     */
    @Suppress("LongParameterList")
    fun entries(
        keys: List<Any>,
        times: List<Long>,
        authors: List<String?>,
        firstUnreadIndex: Int,
        showOlder: Boolean,
        hasHeader: Boolean,
        hasFooter: Boolean,
        groupWindowMillis: Long,
        timeZone: TimeZone,
    ): List<Entry> {
        val rows = ArrayList<Entry>(keys.size * 2 + 2)
        if (hasFooter) rows += Entry.Footer
        for (index in keys.indices.reversed()) {
            val day = startOfDay(times[index], timeZone)
            val newDay = index == 0 || startOfDay(times[index - 1], timeZone) != day
            val unread = index == firstUnreadIndex
            val grouped = !newDay && !unread && grouped(
                previousTime = times[index - 1],
                previousAuthor = authors[index - 1],
                time = times[index],
                author = authors[index],
                groupWindowMillis = groupWindowMillis,
            )
            rows += Entry.Message(index, grouped, keys[index])
            if (unread) rows += Entry.Unread
            if (newDay) rows += Entry.Day(day)
        }
        when {
            showOlder -> rows += Entry.Older
            hasHeader -> rows += Entry.Header
        }
        return rows
    }

    /** Whether a message continues the previous one's group: a known, same author within the window. */
    fun grouped(
        previousTime: Long,
        previousAuthor: String?,
        time: Long,
        author: String?,
        groupWindowMillis: Long,
    ): Boolean = author != null && author == previousAuthor && time - previousTime in 0..groupWindowMillis

    /** Local midnight of [time] in [timeZone], in epoch milliseconds. */
    fun startOfDay(time: Long, timeZone: TimeZone): Long {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * The label of a day separator: [today] or [yesterday] relative to [now],
     * otherwise the weekday and date in [locale] ("Thursday, November 12"),
     * with the year when it is not this year.
     */
    @Suppress("LongParameterList")
    fun dayLabel(
        dayStart: Long,
        now: Long,
        locale: Locale,
        timeZone: TimeZone,
        today: String,
        yesterday: String,
    ): String {
        val todayStart = startOfDay(now, timeZone)
        // One hour into yesterday is always yesterday, whatever daylight saving does.
        val yesterdayStart = startOfDay(todayStart - HOUR_MILLIS, timeZone)
        return when (dayStart) {
            todayStart -> today
            yesterdayStart -> yesterday
            else -> {
                val skeleton = if (year(dayStart, timeZone) == year(now, timeZone)) "EEEEMMMMd" else "yEEEEMMMMd"
                val format = DateFormat.getInstanceForSkeleton(skeleton, locale)
                format.timeZone = android.icu.util.TimeZone.getTimeZone(timeZone.id)
                format.format(dayStart)
            }
        }
    }

    private const val HOUR_MILLIS: Long = 3_600_000L

    private fun year(time: Long, timeZone: TimeZone): Int {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = time
        return calendar.get(Calendar.YEAR)
    }

    /** Whether the reader is at the newest message: the first row of the reversed list, within [thresholdPx]. */
    fun atBottom(firstVisibleIndex: Int, firstVisibleOffset: Int, thresholdPx: Int): Boolean =
        firstVisibleIndex == 0 && firstVisibleOffset <= thresholdPx

    /** Whether to ask for older messages: the top row is within [threshold] rows and nothing is in progress. */
    @Suppress("LongParameterList")
    fun shouldLoadOlder(
        lastVisibleIndex: Int,
        rowCount: Int,
        threshold: Int,
        hasOlder: Boolean,
        loadingOlder: Boolean,
        olderFailed: Boolean,
    ): Boolean = hasOlder && !loadingOlder && !olderFailed && rowCount > 0 &&
        lastVisibleIndex >= rowCount - 1 - threshold.coerceAtLeast(0)

    /**
     * How many messages arrived after [previousNewest], the key that was
     * newest before: the items after it. `0` when there was none (the first
     * load) or it is gone (the list was replaced).
     */
    fun arrived(previousNewest: Any?, keys: List<Any>): Int {
        if (previousNewest == null) return 0
        val at = keys.lastIndexOf(previousNewest)
        return if (at < 0) 0 else keys.size - 1 - at
    }

    /** The colour of day separator labels. */
    fun dayColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the "New messages" label and line. */
    fun unreadColor(colors: MonarchColors): Color = MonarchTonePalette.of(MonarchTone.Accent, colors).strong

    /** The line of the "New messages" divider. */
    fun unreadLineColor(colors: MonarchColors): Color = MonarchTonePalette.of(MonarchTone.Accent, colors).border
}
