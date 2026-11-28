package dev.monarchui.components.collaboration.commentspanel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and pure logic of MonarchCommentsPanel, built from primitives and roles only. */
internal object MonarchCommentsPanelTokens {
    val Inset: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val SmallGap: Dp = MonarchTokens.Space.space1.dp

    /** The placeholder rows while the threads load, as on the web. */
    const val PLACEHOLDER_ROWS: Int = 3

    /** The focus target that stands for the Back button of an open thread. */
    const val BACK_TARGET: String = "\u0000back"

    /** The threads of [filter], in the app's order. */
    fun visible(
        threads: List<MonarchCommentsPanelThread>,
        filter: MonarchCommentsPanelFilter,
    ): List<MonarchCommentsPanelThread> =
        threads.filter { it.resolved == (filter == MonarchCommentsPanelFilter.Resolved) }

    /** The open thread: the selected one when it is listed under the current filter, otherwise none. */
    fun selected(id: String?, visible: List<MonarchCommentsPanelThread>): MonarchCommentsPanelThread? =
        id?.let { wanted -> visible.firstOrNull { it.id == wanted } }

    /** The time shown in the list: [MonarchCommentsPanelThread.updatedAt], or the newest comment's. */
    fun lastActivity(thread: MonarchCommentsPanelThread): Long? =
        thread.updatedAt ?: thread.comments.lastOrNull()?.createdAt

    /**
     * The row that takes focus when the list comes back: [target] while it is listed, the first row when the
     * thread left the list (it was just resolved or reopened), and none when nothing was asked.
     */
    fun focusRow(target: String?, visible: List<MonarchCommentsPanelThread>): String? = when {
        target == null || target == BACK_TARGET -> null
        visible.any { it.id == target } -> target
        else -> visible.firstOrNull()?.id
    }

    /** [time] relative to [now] in [locale], by the shared Monarch rule (the web rule): "20 minutes ago". */
    fun relativeTime(time: Long, now: Long, locale: Locale): String = MonarchRelativeTime.format(time, now, locale)

    /** The line under a thread in the list: the count, then the time when there is one. */
    fun meta(count: String, time: String?): String = listOfNotNull(count, time).joinToString(" · ")

    /** The colour of the quote and the line under a thread. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the author's name and the heading. */
    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    /** The colour of a loading error without a retry. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong
}
