package com.rookhq.monarch.components.intelligence.aistopbutton

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
