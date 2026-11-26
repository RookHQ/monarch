package com.rookhq.monarch.components.datagrid.bulkactionbar

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchBulkActionBarPreview() {
    val actions = listOf(
        MonarchBulkAction(id = "archive", title = "Archive") {},
        MonarchBulkAction(id = "tag", title = "Add tag") {},
        MonarchBulkAction(id = "export", title = "Export") {},
        MonarchBulkAction(id = "delete", title = "Delete", destructive = true) {},
    )
    PreviewFrame {
        MonarchBulkActionBar(count = 3, onClear = {}, totalCount = 120, actions = actions)
        MonarchBulkActionBar(count = 3, onClear = {}, actions = actions, busyActionId = "archive")
        MonarchBulkActionBar(count = 1, onClear = {}, actions = actions.take(2), busy = true)
    }
}
