package com.rookhq.monarch.components.workflow.approvalqueue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The decisions of a MonarchApprovalQueue in progress: the selection, the
 * rows that are busy or failed, the bulk approval and the last announcement.
 */
@Stable
internal class MonarchApprovalQueueDecisions(
    private val scope: CoroutineScope,
    private val handlers: () -> MonarchApprovalQueueHandlers,
    private val text: () -> MonarchApprovalQueueText,
) {
    var selection: Set<String> by mutableStateOf(emptySet())
    var busy: Set<String> by mutableStateOf(emptySet())
        private set
    var failed: Set<String> by mutableStateOf(emptySet())
        private set
    var bulkBusy: Boolean by mutableStateOf(false)
        private set
    var bulkFailed: Boolean by mutableStateOf(false)
        private set
    var announcement: String by mutableStateOf("")

    /** Approves [item] unless it is already busy. */
    fun approve(item: MonarchApprovalQueueItem) {
        val onApprove = handlers().onApprove ?: return
        if (item.id in busy) return
        busy = busy + item.id
        failed = failed - item.id
        scope.launch {
            val outcome = monarchApprovalQueueAttempt { onApprove(item) }
            busy = busy - item.id
            if (outcome) {
                selection = selection - item.id
                announcement = text().approved(item.requesterName)
            } else {
                failed = failed + item.id
                announcement = text().failed
            }
        }
    }

    /** Called before the Send back dialog opens. */
    fun startReject(item: MonarchApprovalQueueItem) {
        failed = failed - item.id
    }

    /** Called after the app sent [item] back. */
    fun rejected(item: MonarchApprovalQueueItem) {
        selection = selection - item.id
        announcement = text().rejected(item.requesterName)
    }

    /** Approves [chosen] together. */
    fun approveMany(chosen: List<MonarchApprovalQueueItem>) {
        val onApproveMany = handlers().onApproveMany ?: return
        if (chosen.isEmpty() || bulkBusy) return
        bulkBusy = true
        bulkFailed = false
        scope.launch {
            val outcome = monarchApprovalQueueAttempt { onApproveMany(chosen) }
            bulkBusy = false
            if (outcome) {
                selection = emptySet()
                announcement = text().bulkApproved(chosen.size)
            } else {
                bulkFailed = true
                announcement = text().failed
            }
        }
    }
}

/** The app's decision handlers, read when a decision starts. */
internal class MonarchApprovalQueueHandlers(
    val onApprove: (suspend (MonarchApprovalQueueItem) -> Unit)?,
    val onReject: (suspend (MonarchApprovalQueueItem, String?) -> Unit)?,
    val onApproveMany: (suspend (List<MonarchApprovalQueueItem>) -> Unit)?,
)

/** Runs [work]: `true` when it returned, `false` when it threw. Cancellation is passed on. */
internal suspend fun monarchApprovalQueueAttempt(work: suspend () -> Unit): Boolean {
    val error = runCatching { work() }.exceptionOrNull()
    if (error is CancellationException) throw error
    return error == null
}

@Composable
internal fun rememberMonarchApprovalQueueDecisions(
    handlers: MonarchApprovalQueueHandlers,
    text: MonarchApprovalQueueText,
): MonarchApprovalQueueDecisions {
    val scope = rememberCoroutineScope()
    val currentHandlers by rememberUpdatedState(handlers)
    val currentText by rememberUpdatedState(text)
    return remember(scope) { MonarchApprovalQueueDecisions(scope, { currentHandlers }, { currentText }) }
}
