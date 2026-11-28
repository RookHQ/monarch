package dev.monarchui.components.files.pagetree

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

private val previewGroups = listOf(
    MonarchPageTreeGroup(
        id = "basics",
        label = "Basics",
        pages = listOf(MonarchPageTreePage("welcome", "Welcome"), MonarchPageTreePage("setup", "Set up")),
    ),
    MonarchPageTreeGroup(id = "guides", label = "Guides", pages = listOf(MonarchPageTreePage("billing", "Billing"))),
    MonarchPageTreeGroup(id = "faq", label = "FAQ", pages = emptyList()),
)

@MonarchPreviews
@Composable
internal fun MonarchPageTreePreview() {
    PreviewFrame {
        MonarchPageTree(
            groups = previewGroups,
            selected = "setup",
            onSelect = {},
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 3),
        )
        MonarchPageTree(
            groups = previewGroups,
            selected = "welcome",
            onSelect = {},
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 4),
            onReorder = { _, _ -> },
            onAddPage = {},
            onAddGroup = {},
            onRenameGroup = { _, _ -> },
            onDeleteGroup = {},
            onDeletePage = {},
        )
    }
}
