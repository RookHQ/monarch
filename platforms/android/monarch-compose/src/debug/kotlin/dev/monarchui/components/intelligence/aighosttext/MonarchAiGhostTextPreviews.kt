package dev.monarchui.components.intelligence.aighosttext

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiGhostTextPreview() {
    PreviewFrame {
        MonarchAiGhostText(
            state = rememberTextFieldState("Thanks for the update. I will"),
            label = "Reply",
            helperText = "Suggestions appear as you type.",
        ) { _, _ -> " send the figures by Friday." }
        MonarchAiGhostText(state = rememberTextFieldState(), label = "Reply", enabled = false) { _, _ -> null }
    }
}
