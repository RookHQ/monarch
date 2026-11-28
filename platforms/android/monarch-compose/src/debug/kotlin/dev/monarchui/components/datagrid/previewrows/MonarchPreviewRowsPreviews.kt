package dev.monarchui.components.datagrid.previewrows

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

private const val PreviewHeightSteps = 4

@MonarchPreviews
@Composable
internal fun MonarchPreviewRowsPreview() {
    PreviewFrame {
        MonarchPreviewRows(
            rows = listOf(
                MonarchPreviewRow(1, mapOf("name" to "Ada Lovelace", "email" to "ada@example.com")),
                MonarchPreviewRow(
                    2,
                    mapOf("name" to "Alan", "email" to "alan@"),
                    errors = listOf(MonarchPreviewRowIssue("Not a valid email address", field = "Email")),
                ),
                MonarchPreviewRow(
                    3,
                    mapOf("name" to "Grace", "email" to null),
                    status = MonarchPreviewRowStatus.Duplicate,
                ),
            ),
            columns = listOf(MonarchPreviewRowsColumn("name", "Name"), MonarchPreviewRowsColumn("email", "Email")),
            label = "Preview of contacts.csv",
            filter = null,
            onFilterChange = {},
            modifier = Modifier.height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
        )
    }
}
