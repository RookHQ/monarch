package com.rookhq.monarch.components.workflow.approvalqueue

import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure logic of MonarchApprovalQueue. */
internal object MonarchApprovalQueueTokens {
    /** The id of the bulk approve action in the bulk action bar. */
    const val BULK_ACTION_ID: String = "approve"

    /** The separator between the title and the time. */
    const val META_DOT: String = "·"

    /** The gap between the toolbar parts and the list. */
    val SectionGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the checkbox, the avatar and the text of a row. */
    val ColumnGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of a row and between the parts of its meta line. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the text and the buttons, and between the buttons. */
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** The space above and below the content of a row. */
    val RowPadding: Dp = MonarchTokens.Space.space3.dp

    /** The space between the edge of a swipe background and its label. */
    val SwipePadding: Dp = MonarchTokens.Space.space5.dp

    /** The space between the parts of a dialog's text. */

    /**
     * The requests whose `searchText` (the requester's name and the title by default) matches [query],
     * case-insensitive and trimmed, then at most [limit] of them.
     */
    fun visible(items: List<MonarchApprovalQueueItem>, query: String, limit: Int?): List<MonarchApprovalQueueItem> {
        val q = query.trim().lowercase()
        val matched = if (q.isEmpty()) {
            items
        } else {
            items.filter { (it.searchText ?: "${it.requesterName} ${it.title}").lowercase().contains(q) }
        }
        return if (limit != null) matched.take(limit.coerceAtLeast(0)) else matched
    }

    /** The selected requests that are still in [items] and can be decided, in the order of [items]. */
    fun chosen(items: List<MonarchApprovalQueueItem>, selection: Set<String>): List<MonarchApprovalQueueItem> =
        items.filter { it.id in selection && it.canDecide }

    /** The state of Select all over the decidable requests among [visible]. */
    fun selectAllState(visible: List<MonarchApprovalQueueItem>, selection: Set<String>): ToggleableState {
        val decidable = visible.filter { it.canDecide }
        val count = decidable.count { it.id in selection }
        return when {
            decidable.isEmpty() || count == 0 -> ToggleableState.Off
            count == decidable.size -> ToggleableState.On
            else -> ToggleableState.Indeterminate
        }
    }

    /** The selection after Select all is tapped: every decidable visible request, or none when all were on. */
    fun toggleAll(visible: List<MonarchApprovalQueueItem>, selection: Set<String>): Set<String> =
        if (selectAllState(visible, selection) == ToggleableState.On) {
            emptySet()
        } else {
            visible.filter { it.canDecide }.map { it.id }.toSet()
        }

    /** [selection] with [id] added or removed. */
    fun toggled(selection: Set<String>, id: String, on: Boolean): Set<String> =
        if (on) selection + id else selection - id

    /** Whether Send back can be confirmed with [reason]. */
    fun canConfirmReject(reason: String, requirement: MonarchApprovalQueueRejectReason): Boolean =
        requirement != MonarchApprovalQueueRejectReason.Required || reason.isNotBlank()

    /** The reason passed to the app: trimmed, or `null` when it is empty or not asked for. */
    fun submittedReason(reason: String, requirement: MonarchApprovalQueueRejectReason): String? =
        if (requirement == MonarchApprovalQueueRejectReason.None) null else reason.trim().ifEmpty { null }

    /** [date] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(date: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(date, now, locale)

    /** What TalkBack reads for a request: the parts that exist, joined with commas. */
    fun spoken(parts: List<String?>): String = parts.filterNotNull().filter { it.isNotBlank() }.joinToString(", ")

    /** The colours of a swipe background: the solid success role for Approve, the solid accent role for Review. */
    fun swipeColors(approve: Boolean, colors: MonarchColors): MonarchSurfaceColors = if (approve) {
        MonarchSurfaceColors(colors.successSolid, colors.successOnSolid, null)
    } else {
        MonarchSurfaceColors(colors.accentSolid, colors.accentOnSolid, null)
    }
}
