package dev.monarchui.components.workflow.approvalqueue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkAction
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkActionBar
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateKind
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.rememberMonarchRelativeClock

private typealias Tokens = MonarchApprovalQueueTokens

/**
 * Requests waiting on the viewer, with Approve, Send back (with a reason)
 * and Review on each, swipe actions, optional search and bulk approval.
 *
 * Built by Monarch as a `LazyColumn` of rows. Each row is a Material 3
 * `SwipeToDismissBox` (swipe toward the start approves, toward the end
 * reviews; the row springs back) over a `MonarchAvatar`, the request's text
 * with `MonarchBadge`s and small `MonarchButton`s, so every swipe action is
 * also a button. Search is a `MonarchSearchBar` above the list, Select all a
 * Material 3 `TriStateCheckbox`, and with [onApproveMany] each row gets a
 * `Checkbox` and a `MonarchBulkActionBar` appears under the list while
 * requests are selected. Send back opens a `MonarchDialog` that asks
 * for a reason (unless [rejectReason] is `None`) and stays open, with a
 * progress button, until [onReject] returns; a failure is shown in the
 * dialog. Bulk approval is confirmed with an `AlertDialog`. Empty and no-match
 * states are `MonarchEmptyState`s.
 *
 * The queue is controlled: the app performs each decision and removes decided
 * requests from [items]. While [onApprove] runs the row's Approve shows
 * progress and its other decisions are unavailable; if it throws, the row
 * says "That did not work. Try again." Give the queue a bounded height (a
 * screen, or `Modifier.height`): it is lazy and does not go inside a
 * vertically scrolling parent. In a loose height it takes the height of its
 * rows.
 *
 * TalkBack reads the list as "Requests waiting for you". Each request is one
 * element read as the requester, detail, title, time, Past due, badges,
 * summary and, after a failure, the error; its buttons and checkbox name the
 * request ("Approve request from Sam Lee"). Approvals, send backs, bulk
 * approvals and failures are announced through a polite live region.
 *
 * @param items the requests, in the order to show them. Ids must be unique. Remove a request once it is decided.
 * @param modifier the modifier of the queue.
 * @param rejectReason whether Send back asks for a reason, and whether it is required.
 * @param searchable adds a search field that filters by requester and title, or by each item's `searchText`.
 * @param limit shows at most this many requests, after the search, for a dashboard card; put "See all" in
 * [footer].
 * @param now the reference time for relative times, in epoch milliseconds. Without it they follow the clock,
 * refreshed every minute.
 * @param onApprove approves one request. Adds Approve (button and swipe). Throwing marks the row as failed.
 * @param onReject sends one request back with the trimmed reason, or `null`. Adds Send back. Throwing keeps the
 * dialog open with the failure.
 * @param onReview opens the full request. Adds Review (button and swipe).
 * @param onApproveMany approves the selected requests after a confirmation. Adds checkboxes, Select all and the
 * bulk action bar. Throwing shows the failure under Select all.
 * @param labels replacements for the built-in strings.
 * @param footer content after the requests, inside the list, such as a "See all requests" button.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchApprovalQueue(
    items: List<MonarchApprovalQueueItem>,
    modifier: Modifier = Modifier,
    rejectReason: MonarchApprovalQueueRejectReason = MonarchApprovalQueueRejectReason.Required,
    searchable: Boolean = false,
    limit: Int? = null,
    now: Long? = null,
    onApprove: (suspend (MonarchApprovalQueueItem) -> Unit)? = null,
    onReject: (suspend (MonarchApprovalQueueItem, String?) -> Unit)? = null,
    onReview: ((MonarchApprovalQueueItem) -> Unit)? = null,
    onApproveMany: (suspend (List<MonarchApprovalQueueItem>) -> Unit)? = null,
    labels: MonarchApprovalQueueLabels = MonarchApprovalQueueLabels(),
    footer: (@Composable () -> Unit)? = null,
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchApprovalQueueText(labels, resources) }
    val decisions = rememberMonarchApprovalQueueDecisions(
        MonarchApprovalQueueHandlers(onApprove, onReject, onApproveMany),
        text,
    )
    var query by rememberSaveable { mutableStateOf("") }
    var rejecting by remember { mutableStateOf<MonarchApprovalQueueItem?>(null) }
    var confirmingBulk by remember { mutableStateOf(false) }
    val clock = rememberMonarchRelativeClock(
        items.map { it.submittedMillis },
        fixed = now,
        running = items.any { it.submittedMillis != null },
    )
    val visible = Tokens.visible(items, query, limit)
    val chosen = Tokens.chosen(items, decisions.selection)
    val selectable = onApproveMany != null
    val actions = MonarchApprovalQueueRowActions(
        onApprove = onApprove?.let { { item -> decisions.approve(item) } },
        onReject = onReject?.let {
            { item ->
                decisions.startReject(item)
                rejecting = item
            }
        },
        onReview = onReview,
        onSelectedChange = if (selectable) {
            { item, on -> decisions.selection = Tokens.toggled(decisions.selection, item.id, on) }
        } else {
            null
        },
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap)) {
        if (searchable && items.isNotEmpty()) {
            MonarchSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = text.search,
            )
        }
        if (selectable && items.isNotEmpty()) {
            MonarchApprovalQueueSelectAll(
                visible = visible,
                selection = decisions.selection,
                onSelectionChange = { decisions.selection = it },
                failed = decisions.bulkFailed,
                text = text,
                idle = chosen.isEmpty(),
            )
        }
        ApprovalQueueList(
            items = items,
            visible = visible,
            decisions = decisions,
            actions = actions,
            now = clock,
            onClearSearch = { query = "" },
            text = text,
            footer = footer,
        )
        if (selectable) {
            MonarchBulkActionBar(
                count = chosen.size,
                onClear = { decisions.selection = emptySet() },
                totalCount = items.count { it.canDecide },
                actions = listOf(
                    MonarchBulkAction(id = Tokens.BULK_ACTION_ID, title = text.approveSelected) {
                        confirmingBulk = true
                    },
                ),
                busyActionId = Tokens.BULK_ACTION_ID.takeIf { decisions.bulkBusy },
            )
        }
        // Always present, so each result is announced once as a change of this live region.
        MonarchLiveStatus(text = decisions.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    val sending = rejecting
    if (sending != null && onReject != null) {
        MonarchApprovalQueueRejectDialog(
            item = sending,
            requirement = rejectReason,
            onReject = onReject,
            onDone = {
                rejecting = null
                decisions.rejected(sending)
            },
            onFailed = { decisions.announcement = text.failed },
            onDismiss = { rejecting = null },
            text = text,
        )
    }
    if (confirmingBulk) {
        MonarchApprovalQueueBulkDialog(
            count = chosen.size,
            onConfirm = {
                confirmingBulk = false
                decisions.approveMany(chosen)
            },
            onDismiss = { confirmingBulk = false },
            text = text,
        )
    }
}

/** The lazy list: the empty state, the no-match state or the requests, then the footer. */
@Suppress("LongParameterList")
@Composable
private fun ColumnScope.ApprovalQueueList(
    items: List<MonarchApprovalQueueItem>,
    visible: List<MonarchApprovalQueueItem>,
    decisions: MonarchApprovalQueueDecisions,
    actions: MonarchApprovalQueueRowActions,
    now: Long,
    onClearSearch: () -> Unit,
    text: MonarchApprovalQueueText,
    footer: (@Composable () -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .semantics { contentDescription = text.label },
    ) {
        when {
            items.isEmpty() -> item(key = EmptyKey) {
                MonarchEmptyState(
                    title = text.emptyTitle,
                    modifier = Modifier.fillMaxWidth(),
                    kind = MonarchEmptyStateKind.Success,
                    size = MonarchEmptyStateSize.Sm,
                    message = text.emptyDescription,
                )
            }
            visible.isEmpty() -> item(key = EmptyKey) {
                MonarchEmptyState(
                    title = text.noMatchesTitle,
                    modifier = Modifier.fillMaxWidth(),
                    kind = MonarchEmptyStateKind.Filtered,
                    size = MonarchEmptyStateSize.Sm,
                    onClearFilters = onClearSearch,
                )
            }
            else -> items(visible, key = { it.id }) { item ->
                if (item !== visible.first()) MonarchDivider(variant = MonarchDividerVariant.Subtle)
                MonarchApprovalQueueRow(
                    item = item,
                    now = now,
                    busy = item.id in decisions.busy,
                    failed = item.id in decisions.failed,
                    selected = item.id in decisions.selection,
                    actions = actions,
                    text = text,
                )
            }
        }
        if (footer != null) item(key = FooterKey) { footer() }
    }
}

private const val EmptyKey = "\u0000empty"
private const val FooterKey = "\u0000footer"
