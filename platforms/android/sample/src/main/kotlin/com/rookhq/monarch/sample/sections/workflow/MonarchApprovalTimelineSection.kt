package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalPerson
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalStepStatus
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalTimeline
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalTimelineStep
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of ApprovalTimeline. */
object MonarchApprovalTimelineSection : CataloguePage {
    override val title = "ApprovalTimeline"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_772_636_400_000L
    private const val HOUR = 3_600_000L
    private const val TWO_DAYS = 48

    @Composable
    override fun Content() {
        var decided by rememberSaveable { mutableStateOf(false) }
        var avatars by rememberSaveable { mutableStateOf(true) }
        val finance = if (decided) {
            MonarchApprovalTimelineStep(
                id = "2",
                label = "Finance check",
                status = MonarchApprovalStepStatus.Approved,
                actor = MonarchApprovalPerson("Jo Park"),
                date = NOW,
            )
        } else {
            MonarchApprovalTimelineStep(
                id = "2",
                label = "Finance check",
                status = MonarchApprovalStepStatus.Pending,
                approvers = listOf(MonarchApprovalPerson("Jo Park"), MonarchApprovalPerson("Kim Cho")),
            )
        }
        CatalogueCaption("An expense in review")
        MonarchSwitchRow(checked = avatars, onCheckedChange = { avatars = it }, label = "Avatars")
        MonarchButton(
            text = if (decided) "Undo the decision" else "Approve the finance check",
            onClick = { decided = !decided },
        )
        MonarchApprovalTimeline(
            steps = listOf(
                MonarchApprovalTimelineStep(
                    id = "0",
                    label = "Submitted",
                    status = MonarchApprovalStepStatus.Submitted,
                    actor = MonarchApprovalPerson("Sam Lee"),
                    date = NOW - TWO_DAYS * HOUR,
                ),
                MonarchApprovalTimelineStep(
                    id = "1",
                    label = "Reporting manager",
                    status = MonarchApprovalStepStatus.Approved,
                    actor = MonarchApprovalPerson("Ana Ruiz"),
                    date = NOW - HOUR,
                    comment = "Fine by me.",
                ),
                finance,
                MonarchApprovalTimelineStep(id = "3", label = "Payout", status = MonarchApprovalStepStatus.Waiting),
            ),
            now = NOW,
            showAvatars = avatars,
        )
        CatalogueCaption("Rejected, skipped and cancelled")
        MonarchApprovalTimeline(
            steps = listOf(
                MonarchApprovalTimelineStep(
                    id = "0",
                    label = "Legal review",
                    status = MonarchApprovalStepStatus.Rejected,
                    actor = MonarchApprovalPerson("Ben Okafor"),
                    date = NOW - HOUR,
                    comment = "The contract term is missing.",
                ),
                MonarchApprovalTimelineStep(
                    id = "1",
                    label = "Security review",
                    status = MonarchApprovalStepStatus.Skipped,
                    note = "Not needed for this amount.",
                ),
                MonarchApprovalTimelineStep(
                    id = "2",
                    label = "Payout",
                    status = MonarchApprovalStepStatus.Cancelled,
                    note = "Withdrawn by the requester.",
                ),
            ),
            now = NOW,
        )
        CatalogueCaption("Not submitted")
        MonarchApprovalTimeline(steps = emptyList())
    }
}
