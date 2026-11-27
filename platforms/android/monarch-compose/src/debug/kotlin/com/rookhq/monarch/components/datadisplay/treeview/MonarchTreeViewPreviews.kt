package com.rookhq.monarch.components.datadisplay.treeview

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon
import com.rookhq.monarch.theme.MonarchTheme

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
