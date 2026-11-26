package com.rookhq.monarch.components.files.filelist

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkAction
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

private val previewFiles = listOf(
    MonarchFileListItem(id = "1", name = "Q3 report.pdf", sizeBytes = 2_400_000, meta = "Edited yesterday"),
    MonarchFileListItem(id = "2", name = "Budget 2027.xlsx", sizeBytes = 48_000, badges = listOf("Shared")),
    MonarchFileListItem(id = "3", name = "Old logo.png", sizeBytes = 12_000, enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchFileListPreview() {
    PreviewFrame {
        MonarchFileList(
            items = previewFiles,
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 3),
            selected = setOf("1"),
            onSelectedChange = {},
            bulkActions = listOf(MonarchBulkAction(id = "delete", title = "Delete", destructive = true) {}),
        )
        MonarchFileList(
            items = previewFiles,
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 3),
            layout = MonarchFileListLayout.Grid,
        )
        MonarchFileList(items = emptyList())
    }
}
