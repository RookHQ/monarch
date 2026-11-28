package dev.monarchui.components.intelligence.aistopbutton

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiStopButtonPreview() {
    PreviewFrame {
        MonarchAiStopButton(onStop = {})
        MonarchAiStopButton(onStop = {}, showLabel = false)
        MonarchAiStopButton(onStop = {}, enabled = false)
        MonarchAiStopButton(onStop = {}, stopped = true, onContinue = {})
    }
}
