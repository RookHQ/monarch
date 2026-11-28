package dev.monarchui.components.feedback.consequencelist

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

private val PreviewItems = listOf(
    MonarchConsequence(MonarchConsequenceKind.Removed, "tasks will be deleted", count = 1284),
    MonarchConsequence(MonarchConsequenceKind.Changed, "Members lose access immediately"),
    MonarchConsequence(MonarchConsequenceKind.Warning, "This cannot be undone", detail = "Export first if needed."),
    MonarchConsequence(MonarchConsequenceKind.Kept, "Invoices stay in billing"),
)

@MonarchPreviews
@Composable
internal fun MonarchConsequenceListPreview() {
    PreviewFrame {
        MonarchConsequenceList(
            items = PreviewItems,
            heading = "What happens",
            icon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
        )
        MonarchConsequenceList(items = PreviewItems, grouped = true)
    }
}

@MonarchPreviews
@Composable
internal fun MonarchConsequenceListLoadingPreview() {
    PreviewFrame { MonarchConsequenceList(items = emptyList(), loading = true) }
}
