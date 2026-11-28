package dev.monarchui.components.collaboration.recordingindicator

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRecordingIndicatorPreview() {
    PreviewFrame {
        MonarchRecordingIndicatorVariant.entries.forEach { variant ->
            MonarchRecordingIndicator(recording = true, variant = variant, elapsedMillis = 83_000L, onStop = {})
            MonarchRecordingIndicator(recording = true, variant = variant, paused = true, elapsedMillis = 83_000L)
        }
        MonarchRecordingIndicator(recording = true)
    }
}
