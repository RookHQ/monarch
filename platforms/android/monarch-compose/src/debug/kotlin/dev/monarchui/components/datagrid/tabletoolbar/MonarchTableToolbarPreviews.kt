package dev.monarchui.components.datagrid.tabletoolbar

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkAction
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
