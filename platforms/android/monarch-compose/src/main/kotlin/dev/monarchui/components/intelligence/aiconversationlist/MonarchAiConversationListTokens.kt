package dev.monarchui.components.intelligence.aiconversationlist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** The sizes, colours and pure rules of MonarchAiConversationList. */
internal object MonarchAiConversationListTokens {
    /** The padding of a row. */
    val RowPadding = MonarchTokens.Space.space2.dp

    /** The width of the marker of the open conversation. */
    val MarkerWidth = MonarchTokens.Space.space1.dp

    /** The gap between the marker, the text and the menu button. */
    val RowGap = MonarchTokens.Space.space2.dp

    /** The padding of a group heading. */
    val HeadingPadding = MonarchTokens.Space.space2.dp

    /** The gap between the search field, the new button and the list. */
    val Gap = MonarchTokens.Space.space2.dp

    /** The most lines of a title. */
    const val TitleLines = 2

    /** The days before today that are in [MonarchAiConversationGroup.Previous7Days]. */
    const val RecentDays = 7

    /** The group of a conversation, by the calendar days of [timeZone] counted from [nowMillis]. */
    fun group(summary: MonarchAiConversationSummary, nowMillis: Long, timeZone: TimeZone): MonarchAiConversationGroup {
        if (summary.pinned) return MonarchAiConversationGroup.Pinned
        val today = startOfDay(nowMillis, timeZone, daysBack = 0)
        return when {
            summary.updatedAtMillis >= today -> MonarchAiConversationGroup.Today
            summary.updatedAtMillis >= startOfDay(nowMillis, timeZone, RecentDays) ->
                MonarchAiConversationGroup.Previous7Days
            else -> MonarchAiConversationGroup.Older
        }
    }

    /**
     * The conversations in their groups, groups in order and empty ones left out, newest first in each group,
     * keeping only the first [limit] rows.
     */
    fun grouped(
        conversations: List<MonarchAiConversationSummary>,
        nowMillis: Long,
        timeZone: TimeZone,
        limit: Int,
    ): List<Pair<MonarchAiConversationGroup, List<MonarchAiConversationSummary>>> {
        val ordered = conversations
            .map { Placed(it, group(it, nowMillis, timeZone)) }
            .sortedWith(compareBy<Placed> { it.group.ordinal }.thenByDescending { it.summary.updatedAtMillis })
            .take(limit.coerceAtLeast(0))
        return ordered.groupBy({ it.group }, { it.summary }).toList()
    }

    /** The conversations whose title or preview contains [query], ignoring case and spaces around it. */
    fun filter(conversations: List<MonarchAiConversationSummary>, query: String): List<MonarchAiConversationSummary> {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return conversations
        return conversations.filter {
            it.title.lowercase(Locale.ROOT).contains(needle) ||
                it.preview.orEmpty().lowercase(Locale.ROOT).contains(needle)
        }
    }

    /** The new title to save, trimmed, or `null` when it is empty or unchanged. */
    fun renamed(old: String, new: String): String? = new.trim().takeIf { it.isNotEmpty() && it != old.trim() }

    /** The title to show: [untitled] for an empty or white-space title. */
    fun title(title: String, untitled: String): String = title.ifBlank { untitled }

    /** The marker of the open conversation. */
    fun marker(colors: MonarchColors): Color = colors.accentSolid

    /** The fill of the open conversation's row. */
    fun activeBackground(colors: MonarchColors): Color = colors.bgSubtle

    /** The background of a sticky group heading, so rows do not show through it. */
    fun headingBackground(colors: MonarchColors): Color = colors.bgSurface

    /** The colour of previews and group headings. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    private class Placed(val summary: MonarchAiConversationSummary, val group: MonarchAiConversationGroup)

    private fun startOfDay(millis: Long, timeZone: TimeZone, daysBack: Int): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysBack)
        }.timeInMillis
}
