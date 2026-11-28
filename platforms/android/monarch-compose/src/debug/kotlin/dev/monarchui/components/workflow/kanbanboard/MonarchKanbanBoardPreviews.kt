package dev.monarchui.components.workflow.kanbanboard

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

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
