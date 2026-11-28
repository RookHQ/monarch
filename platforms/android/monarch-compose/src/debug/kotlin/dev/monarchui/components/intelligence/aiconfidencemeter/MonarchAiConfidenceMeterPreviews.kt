package dev.monarchui.components.intelligence.aiconfidencemeter

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
