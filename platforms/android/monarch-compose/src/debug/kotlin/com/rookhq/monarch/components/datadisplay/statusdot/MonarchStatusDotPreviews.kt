package com.rookhq.monarch.components.datadisplay.statusdot

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchStatusDotPreview() {
    PreviewFrame {
        MonarchStatusDotSize.entries.forEach { size ->
            MonarchTone.entries.forEach { tone -> MonarchStatusDot(label = tone.name, tone = tone, size = size) }
        }
        MonarchStatusDot(label = "Recording", tone = MonarchTone.Danger, pulsing = true)
        MonarchStatusDot(label = "Online", tone = MonarchTone.Success, labelHidden = true)
    }
}
