package com.rookhq.monarch.components.datagrid.tabletoolbar

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.datadisplay.tag.MonarchChip
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkAction
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTableToolbarPreview() {
    PreviewFrame {
        MonarchTableToolbar(
            resultCount = 1204,
            filters = {
                MonarchChip(text = "Active", selected = true, onSelectedChange = {})
                MonarchChip(text = "Owner: me", selected = false, onSelectedChange = {})
            },
        ) { MonarchButton(text = "Columns", onClick = {}, size = MonarchButtonSize.Sm) }
        MonarchTableToolbar(
            resultCount = 40,
            selectedCount = 3,
            totalCount = 40,
            bulkActions = listOf(MonarchBulkAction(id = "delete", title = "Delete", destructive = true) {}),
            onClearSelection = {},
        ) { MonarchButton(text = "Columns", onClick = {}, size = MonarchButtonSize.Sm) }
    }
}
