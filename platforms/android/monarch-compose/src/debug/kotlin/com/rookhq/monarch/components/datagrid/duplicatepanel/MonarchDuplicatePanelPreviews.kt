package com.rookhq.monarch.components.datagrid.duplicatepanel

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
