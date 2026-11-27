@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.workflow.issuecard.MonarchIssueCard
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanBoard
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanColumn
import com.rookhq.monarch.components.workflow.prioritypicker.MonarchPriorityLevel
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of KanbanBoard: long press a card to drag it, or use its Move button. */
object MonarchKanbanBoardSection : CataloguePage {
    override val title = "KanbanBoard"

    private const val SAVE_MILLIS = 400L
    private const val HEIGHT_STEPS = 6
    private const val DOING_LIMIT = 2

    private class Issue(val key: String, val title: String, val priority: MonarchPriorityLevel)

    private fun start(): List<MonarchKanbanColumn<Issue>> = listOf(
        MonarchKanbanColumn(
            id = "todo",
            title = "To do",
            items = listOf(
                Issue("ABC-12", "Fix login on slow networks", MonarchPriorityLevel.High),
                Issue("ABC-15", "Export to CSV", MonarchPriorityLevel.Medium),
            ),
        ),
        MonarchKanbanColumn(
            id = "doing",
            title = "Doing",
            items = listOf(Issue("ABC-18", "Billing page", MonarchPriorityLevel.Urgent)),
            wipLimit = DOING_LIMIT,
        ),
        MonarchKanbanColumn(id = "done", title = "Done", items = emptyList()),
    )

    @Composable
    override fun Content() {
        var columns by remember { mutableStateOf(start()) }
        CatalogueCaption("A board with a WIP limit on Doing; saving takes a moment")
        MonarchKanbanBoard(
            columns = columns,
            key = { it.key },
            itemLabel = { it.key },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            onMove = { _, next ->
                columns = next
                delay(SAVE_MILLIS)
            },
            enforceWipLimits = true,
        ) { issue, state ->
            MonarchIssueCard(
                title = issue.title,
                key = issue.key,
                priority = issue.priority,
                dragging = state.dragging,
            )
        }
    }
}
