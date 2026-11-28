@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.workflow.issuecard.MonarchIssueCard
import dev.monarchui.components.workflow.issuecard.MonarchIssueCardAssignee
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityLevel
import dev.monarchui.components.workflow.statuspicker.MonarchStatusCategory
import dev.monarchui.components.workflow.statuspicker.MonarchStatusOption
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val IssueStatuses = listOf(
    MonarchStatusOption("todo", "Todo", MonarchStatusCategory.Unstarted),
    MonarchStatusOption("doing", "In progress", MonarchStatusCategory.Started),
    MonarchStatusOption("review", "In review", MonarchStatusCategory.Started),
    MonarchStatusOption("done", "Done", MonarchStatusCategory.Completed),
)

/** 12 March 2026 at midnight UTC: overdue on any later day. */
private const val SampleDue = 1_773_273_600_000L

/** The catalogue page of IssueCard. */
object MonarchIssueCardSection : CataloguePage {
    override val title = "IssueCard"

    @Composable
    override fun Content() {
        var status by rememberSaveable { mutableStateOf("doing") }
        var priority by rememberSaveable { mutableStateOf(MonarchPriorityLevel.High) }
        var selected by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Editable status and priority")
        MonarchIssueCard(
            title = "Sign-in fails on slow networks",
            key = "STR-142",
            status = IssueStatuses.firstOrNull { it.id == status },
            statuses = IssueStatuses,
            priority = priority,
            assignee = MonarchIssueCardAssignee.Person("Ana Ruiz"),
            tags = listOf("Bug", "Auth"),
            dueDate = SampleDue,
            selected = selected,
            onStatusChange = { status = it },
            onPriorityChange = { priority = it },
            onOpen = { selected = !selected },
        )
        CatalogueCaption("Read-only, unassigned")
        MonarchIssueCard(
            title = "Write the release notes",
            key = "STR-150",
            status = IssueStatuses.first(),
            priority = MonarchPriorityLevel.Low,
            assignee = MonarchIssueCardAssignee.Unassigned,
        )
        CatalogueCaption("Being dragged")
        MonarchIssueCard(title = "Move me to In review", key = "STR-151", dragging = true)
    }
}
