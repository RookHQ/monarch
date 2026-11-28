package dev.monarchui.components.datadisplay.progress

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchProgressPreview() {
    PreviewFrame {
        MonarchProgress(progress = 0.6f, label = "Uploading report")
        MonarchProgress()
        MonarchProgress(progress = 0.6f, variant = MonarchProgressVariant.Circular, label = "Uploading report")
        MonarchProgress(variant = MonarchProgressVariant.Circular)
    }
}
