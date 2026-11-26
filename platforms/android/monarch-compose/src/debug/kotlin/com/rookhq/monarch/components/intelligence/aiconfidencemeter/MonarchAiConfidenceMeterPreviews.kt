package com.rookhq.monarch.components.intelligence.aiconfidencemeter

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiConfidenceMeterPreview() {
    PreviewFrame {
        listOf(0.42f, 0.72f, 0.91f).forEach { value ->
            MonarchAiConfidenceMeter(value = value)
            MonarchAiConfidenceMeter(value = value, variant = MonarchAiConfidenceMeterVariant.Bar)
        }
        MonarchAiConfidenceMeter(
            value = 0.91f,
            variant = MonarchAiConfidenceMeterVariant.Bar,
            size = MonarchAiConfidenceMeterSize.Sm,
            explanation = "Matches two sources.",
        )
    }
}
