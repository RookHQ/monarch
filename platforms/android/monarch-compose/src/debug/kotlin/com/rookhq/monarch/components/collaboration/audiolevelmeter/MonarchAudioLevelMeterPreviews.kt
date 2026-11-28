package com.rookhq.monarch.components.collaboration.audiolevelmeter

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAudioLevelMeterPreview() {
    PreviewFrame {
        listOf(0f, 0.3f, 0.6f, 1f).forEach { level ->
            MonarchAudioLevelMeter(level = level)
        }
        MonarchAudioLevelMeter(level = 0.6f, size = MonarchAudioLevelMeterSize.Sm, segments = 8)
        MonarchAudioLevelMeter(level = 0.6f, muted = true)
        MonarchAudioLevelMeter(level = 0.6f, variant = MonarchAudioLevelMeterVariant.Bar)
        MonarchAudioLevelMeter(level = 0.6f, variant = MonarchAudioLevelMeterVariant.Bar, muted = true)
    }
}
