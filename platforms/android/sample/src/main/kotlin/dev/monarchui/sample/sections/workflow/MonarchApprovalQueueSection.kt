@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.workflow

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.workflow.approvalqueue.MonarchApprovalQueue
import dev.monarchui.components.workflow.approvalqueue.MonarchApprovalQueueItem
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import java.io.IOException
import kotlinx.coroutines.delay

/** The catalogue page of ApprovalQueue: decisions take a moment, and Sam Lee's approval fails once. */
object MonarchApprovalQueueSection : CataloguePage {
    override val title = "ApprovalQueue"

    private const val WORK_MILLIS = 700L
    private const val HOUR = 3_600_000L
    private const val LIST_HEIGHT_STEPS = 6
    private const val CARD_HEIGHT_STEPS = 3

    private fun requests(now: Long) = listOf(
        MonarchApprovalQueueItem(
            id = "1",
            requesterName = "Alex Morgan",
            title = "Expense claim",
            requesterDetail = "Design",
            summary = "3 receipts, travel",
            submittedMillis = now - 50 * HOUR,
            overdue = true,
        ),
        MonarchApprovalQueueItem(
            id = "2",
            requesterName = "Sam Lee",
            title = "Leave, 2 days",
            submittedMillis = now - 2 * HOUR,
            badges = listOf(MonarchStatusDefinition(label = "Resubmitted", tone = MonarchTone.Info)),
        ),
        MonarchApprovalQueueItem(
            id = "3",
            requesterName = "Jo Park",
            title = "Timesheet",
            summary = "Week 46, 38 hours",
            submittedMillis = now - HOUR,
        ),
        MonarchApprovalQueueItem(
            id = "4",
            requesterName = "Rui Costa",
            title = "Access to Billing",
            requesterDetail = "Finance",
            canDecide = false,
        ),
    )

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var items by remember { mutableStateOf(requests(now)) }
        var failedOnce by remember { mutableStateOf(false) }
        val remove: (Collection<String>) -> Unit = { ids -> items = items.filterNot { it.id in ids } }
        CatalogueCaption("An approvals screen")
        MonarchApprovalQueue(
            items = items,
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS),
            searchable = true,
            onApprove = { item ->
                delay(WORK_MILLIS)
                if (item.id == "2" && !failedOnce) {
                    failedOnce = true
                    throw IOException("The server did not answer")
                }
                remove(listOf(item.id))
            },
            onReject = { item, _ ->
                delay(WORK_MILLIS)
                remove(listOf(item.id))
            },
            onReview = {},
            onApproveMany = { chosen ->
                delay(WORK_MILLIS)
                remove(chosen.map { it.id })
            },
        )
        MonarchButton(
            text = "Start again",
            onClick = {
                items = requests(now)
                failedOnce = false
            },
        )
        CatalogueCaption("A dashboard card")
        MonarchApprovalQueue(
            items = requests(now),
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * CARD_HEIGHT_STEPS),
            limit = 1,
            onApprove = {},
        ) {
            MonarchButton(text = "See all requests", onClick = {}, variant = MonarchButtonVariant.Ghost)
        }
    }
}
