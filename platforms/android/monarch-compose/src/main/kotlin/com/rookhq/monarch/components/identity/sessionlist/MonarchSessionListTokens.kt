package com.rookhq.monarch.components.identity.sessionlist

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardState
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure logic of MonarchSessionList. */
internal object MonarchSessionListTokens {
    /** The separator between the parts of a line. */
    const val SEPARATOR: String = " · "

    /** The gap between the notice and the list, and between rows. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the icon, the text and the actions. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of a row. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The tone of the This device badge. */
    val CurrentTone: MonarchTone = MonarchTone.Success

    /** [date] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(date: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(date, now, locale)

    /** The parts that exist, joined with the separator, or `null` when there are none. */
    fun line(vararg parts: String?): String? =
        parts.filterNotNull().filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR)

    /** The state of the section card: loading, error, empty for a ready list without sessions, or ready. */
    fun cardState(state: MonarchSessionListState, empty: Boolean): MonarchSectionCardState = when {
        state == MonarchSessionListState.Loading -> MonarchSectionCardState.Loading
        state == MonarchSessionListState.Failed -> MonarchSectionCardState.Error
        empty -> MonarchSectionCardState.Empty
        else -> MonarchSectionCardState.Ready
    }

    /** The ids of every session except the current one, in order. */
    fun otherIds(sessions: List<MonarchSession>): List<String> = sessions.filterNot { it.current }.map { it.id }

    /** Whether the header shows Sign out other devices: the app handles it, the list is ready and has others. */
    fun showsSignOutOthers(handled: Boolean, ready: Boolean, sessions: List<MonarchSession>): Boolean =
        handled && ready && sessions.any { !it.current }

    /** What TalkBack reads after a session's label: the badge, the detail line and the times. */
    fun spokenDetails(badge: String?, detail: String?, times: String?): String =
        listOfNotNull(badge, detail, times).joinToString(", ")
}
