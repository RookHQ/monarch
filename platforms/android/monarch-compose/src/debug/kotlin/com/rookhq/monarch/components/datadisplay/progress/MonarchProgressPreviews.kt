package com.rookhq.monarch.components.datadisplay.progress

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
