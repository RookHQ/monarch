package dev.monarchui.components.intelligence.aimodeswitcher

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
