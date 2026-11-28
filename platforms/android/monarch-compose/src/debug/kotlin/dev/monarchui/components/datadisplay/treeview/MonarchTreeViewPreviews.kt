package dev.monarchui.components.datadisplay.treeview

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon
import dev.monarchui.theme.MonarchTheme

private val previewTree = listOf(
    MonarchTreeViewItem(
        id = "eng",
        label = "Engineering",
        description = "Department · 12 people",
        children = listOf(
            MonarchTreeViewItem(id = "web", label = "Web"),
            MonarchTreeViewItem(id = "mobile", label = "Mobile", hasChildren = true),
        ),
    ),
    MonarchTreeViewItem(id = "sales", label = "Sales", children = emptyList()),
    MonarchTreeViewItem(id = "legal", label = "Legal", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchTreeViewPreview() {
    PreviewFrame {
        MonarchTreeView(
            items = previewTree,
            expanded = setOf("eng", "mobile"),
            onExpandedChange = {},
            expandIcon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 3),
            selected = setOf("web"),
            loading = setOf("mobile"),
        )
    }
}
