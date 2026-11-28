package dev.monarchui.components.workflow.statuspicker

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewStatuses = listOf(
    MonarchStatusOption("triage", "Triage", MonarchStatusCategory.Backlog),
    MonarchStatusOption("todo", "Todo", MonarchStatusCategory.Unstarted),
    MonarchStatusOption("doing", "In progress", MonarchStatusCategory.Started),
    MonarchStatusOption("done", "Done", MonarchStatusCategory.Completed),
    MonarchStatusOption("dropped", "Won't do", MonarchStatusCategory.Cancelled),
)

@MonarchPreviews
@Composable
internal fun MonarchStatusPickerPreview() {
    PreviewFrame {
        PreviewStatuses.forEach { status ->
            MonarchStatusPicker(statuses = PreviewStatuses, value = status.id, onValueChange = {})
        }
        MonarchStatusPicker(
            statuses = PreviewStatuses,
            value = "doing",
            onValueChange = {},
            size = MonarchStatusPickerSize.Sm,
        )
        MonarchStatusPicker(statuses = PreviewStatuses, value = "done", onValueChange = {}, showLabel = false)
        MonarchStatusPicker(statuses = PreviewStatuses, value = null, onValueChange = {}, enabled = false)
        Row { MonarchStatusCategory.entries.forEach { MonarchStatusGlyph(it) } }
    }
}
