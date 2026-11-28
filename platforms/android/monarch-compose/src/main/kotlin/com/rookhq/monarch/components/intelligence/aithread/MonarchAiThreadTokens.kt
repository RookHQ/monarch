package com.rookhq.monarch.components.intelligence.aithread

import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationStatus
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiErrorPart
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** The notice under the messages of a MonarchAiThread, the most important first. */
internal enum class AiThreadNoticeKind { Offline, Sending, Slow }

/** Component tokens and the pure logic of MonarchAiThread. */
internal object MonarchAiThreadTokens {
    /** The space between messages. */
    val MessageGap: Dp = MonarchTokens.Space.space4.dp

    /** The space around the jump button. */
    val JumpPadding: Dp = MonarchTokens.Space.space3.dp

    /** The space between the jump button's label and its count. */
    val JumpGap: Dp = MonarchTokens.Space.space2.dp

    /** The space above the notice. */
    val NoticePadding: Dp = MonarchTokens.Space.space2.dp

    /** How far from the bottom still counts as at the bottom: 48 pixels on the web. */
    val AtBottomThreshold: Dp = MonarchTokens.Space.space12.dp

    /** The date skeleton of a day in this year: weekday, month and day. */
    const val THIS_YEAR_SKELETON: String = "EEEEMMMMd"

    /** The date skeleton of a day in another year: with the year. */
    const val OTHER_YEAR_SKELETON: String = "yEEEEMMMMd"

    private const val DAY_KEY_PREFIX = "monarch-aithread-day-"
    private const val HOUR_MILLIS = 3_600_000L

    /** One row of the lazy list, newest first. */
    sealed interface Entry {
        /** The lazy list key: stable while the row's content is the same. */
        val key: Any

        /** A message, by its index in the messages, oldest first. */
        data class Message(val index: Int, override val key: Any) : Entry

        /** The day label above the first message of a day; [dayStart] is midnight in the thread's zone. */
        data class Day(val dayStart: Long, val firstId: String) : Entry {
            override val key: Any get() = "$DAY_KEY_PREFIX$dayStart-$firstId"
        }

        /** The thinking indicator under the messages while a reply is submitted. */
        data object Thinking : Entry {
            override val key: Any get() = EntryKey.Thinking
        }

        /** "Load earlier messages" above the messages. */
        data object Earlier : Entry {
            override val key: Any get() = EntryKey.Earlier
        }
    }

    /** Keys of the rows that are not messages: an enum, so it can be saved in a Bundle and no message id equals it. */
    enum class EntryKey { Thinking, Earlier }

    /** Whether a reply is running: submitted or streaming. */
    fun busy(status: MonarchAiConversationStatus): Boolean =
        status == MonarchAiConversationStatus.Submitted || status == MonarchAiConversationStatus.Streaming

    /**
     * The rows of the list, newest first for a `reverseLayout` list: the
     * thinking indicator while [thinking], then each message preceded (above
     * it) by a day label when its day differs from the message before it, then
     * "Load earlier" while [showEarlier]. A message without a valid time gets
     * no day label, and the message after it starts a new day, as on the web.
     */
    @Suppress("LongParameterList")
    fun entries(
        ids: List<String>,
        times: List<Long?>,
        daySeparators: Boolean,
        thinking: Boolean,
        showEarlier: Boolean,
        timeZone: TimeZone,
    ): List<Entry> {
        val oldestFirst = ArrayList<Entry>(ids.size * 2 + 2)
        var previousDay: Long? = null
        ids.forEachIndexed { index, id ->
            if (daySeparators) {
                val day = times[index]?.let { startOfDay(it, timeZone) }
                if (day != null && day != previousDay) oldestFirst += Entry.Day(day, id)
                previousDay = day
            }
            oldestFirst += Entry.Message(index, id)
        }
        val rows = ArrayList<Entry>(oldestFirst.size + 2)
        if (thinking) rows += Entry.Thinking
        rows.addAll(oldestFirst.asReversed())
        if (showEarlier) rows += Entry.Earlier
        return rows
    }

    /** Midnight of the day of [millis] in [timeZone]. */
    fun startOfDay(millis: Long, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).run {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun year(millis: Long, timeZone: TimeZone): Int =
        Calendar.getInstance(timeZone).run {
            timeInMillis = millis
            get(Calendar.YEAR)
        }

    /** "Today", "Yesterday", a weekday and date in this year, or a full date, in [timeZone]. */
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
                val skeleton = if (year(dayStart, timeZone) == year(now, timeZone)) {
                    THIS_YEAR_SKELETON
                } else {
                    OTHER_YEAR_SKELETON
                }
                DateFormat.getInstanceForSkeleton(skeleton, locale).apply { this.timeZone = timeZone }.format(dayStart)
            }
        }
    }

    /** Whether the list shows its bottom: the newest row, scrolled less than the threshold. */
    fun atBottom(firstVisibleIndex: Int, firstVisibleOffset: Int, thresholdPx: Int): Boolean =
        firstVisibleIndex == 0 && firstVisibleOffset <= thresholdPx

    /** How many messages arrived after [lastSeenId], the newest one the person saw. `0` when it is gone. */
    fun newCount(ids: List<String>, lastSeenId: String?): Int {
        if (lastSeenId == null) return 0
        val index = ids.indexOf(lastSeenId)
        return if (index < 0) 0 else ids.size - 1 - index
    }

    /** The notice to show: offline first, then queued messages being sent, then a slow running reply. */
    fun notice(offline: Boolean, queuedCount: Int, slow: Boolean, busy: Boolean): AiThreadNoticeKind? = when {
        offline -> AiThreadNoticeKind.Offline
        queuedCount > 0 -> AiThreadNoticeKind.Sending
        slow && busy -> AiThreadNoticeKind.Slow
        else -> null
    }

    /** The tone of a notice: a warning while the connection is the problem, neutral while the reply is slow. */
    fun noticeTone(kind: AiThreadNoticeKind): MonarchTone = when (kind) {
        AiThreadNoticeKind.Offline, AiThreadNoticeKind.Sending -> MonarchTone.Warning
        AiThreadNoticeKind.Slow -> MonarchTone.Neutral
    }

    /** Whether the empty content replaces the list: no messages, no submitted reply and nothing to load. */
    fun showsEmpty(messageCount: Int, thinking: Boolean, showEarlier: Boolean, hasEmpty: Boolean): Boolean =
        hasEmpty && messageCount == 0 && !(thinking || showEarlier)

    /** Whether a conversation error is announced: when the newest message does not show an error itself. */
    fun announcesError(hasError: Boolean, last: MonarchAiConversationMessage?): Boolean =
        hasError && last?.parts?.none { it is MonarchAiErrorPart } != false
}
