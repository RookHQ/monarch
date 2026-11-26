package com.rookhq.monarch.components.workflow.kanbanboard

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

private const val PreviewHeightSteps = 5

private val PreviewColumns = listOf(
    MonarchKanbanColumn("todo", "To do", listOf("Fix login", "Export CSV")),
    MonarchKanbanColumn("doing", "Doing", listOf("Billing page", "Search", "Audit log"), wipLimit = 2),
    MonarchKanbanColumn("done", "Done", emptyList()),
)

@MonarchPreviews
@Composable
internal fun MonarchKanbanBoardPreview() {
    PreviewFrame {
        MonarchKanbanBoard(
            columns = PreviewColumns,
            key = { it },
            itemLabel = { it },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
            onMove = { _, _ -> },
        ) { card, _ -> MonarchText(text = card) }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchKanbanBoardReadOnlyPreview() {
    PreviewFrame {
        MonarchKanbanBoard(
            columns = PreviewColumns.drop(1),
            key = { it },
            itemLabel = { it },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
        ) { card, _ -> MonarchText(text = card) }
    }
}
