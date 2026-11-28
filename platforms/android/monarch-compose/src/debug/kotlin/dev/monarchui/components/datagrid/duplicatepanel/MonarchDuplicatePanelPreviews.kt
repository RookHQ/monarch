package dev.monarchui.components.datagrid.duplicatepanel

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

private val PreviewMatches = listOf(
    MonarchDuplicateMatch(
        id = "1",
        title = "Example Ltd",
        kind = "Client",
        reasons = listOf("Same email", "Same company domain"),
        score = 0.92f,
    ),
    MonarchDuplicateMatch(id = "2", title = "Example Limited", detail = "Archived"),
)

@MonarchPreviews
@Composable
internal fun MonarchDuplicatePanelHintPreview() {
    PreviewFrame {
        MonarchDuplicatePanel(
            matches = PreviewMatches,
            footer = "You can still create it.",
            icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            onOpen = {},
            onMerge = {},
            onDismiss = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchDuplicatePanelPanelPreview() {
    PreviewFrame {
        MonarchDuplicatePanel(
            matches = PreviewMatches.take(1),
            variant = MonarchDuplicatePanelVariant.Panel,
            onOpen = {},
        )
    }
}
