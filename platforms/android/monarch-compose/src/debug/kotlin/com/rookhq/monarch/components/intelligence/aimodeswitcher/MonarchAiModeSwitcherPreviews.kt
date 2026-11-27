package com.rookhq.monarch.components.intelligence.aimodeswitcher

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiModeSwitcherPreview() {
    PreviewFrame {
        MonarchAiModeSwitcher(selection = "ask", onSelectionChange = {})
        MonarchAiModeSwitcher(
            selection = "edit",
            onSelectionChange = {},
            size = MonarchAiModeSwitcherSize.Md,
            showDescription = false,
        )
        MonarchAiModeSwitcher(selection = "agent", onSelectionChange = {}, enabled = false)
    }
}
