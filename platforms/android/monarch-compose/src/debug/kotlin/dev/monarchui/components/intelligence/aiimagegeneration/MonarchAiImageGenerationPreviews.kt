package dev.monarchui.components.intelligence.aiimagegeneration

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchAiImageGenerationPreview() {
    val picture = ColorPainter(MonarchTheme.colors.aiSubtle)
    PreviewFrame {
        MonarchAiImageGeneration(
            images = listOf(
                MonarchAiGeneratedImage("a", picture, "A lighthouse at dusk"),
                MonarchAiGeneratedImage("b", picture),
            ),
            status = MonarchAiImageGenerationStatus.Done,
            onAltChange = { _, _ -> },
            selection = "a",
            onSelect = {},
            onVariations = {},
        )
        MonarchAiImageGeneration(
            images = listOf(MonarchAiGeneratedImage("a")),
            status = MonarchAiImageGenerationStatus.Pending,
            onAltChange = { _, _ -> },
            progress = 0.4f,
        )
        MonarchAiImageGeneration(
            images = emptyList(),
            status = MonarchAiImageGenerationStatus.Error,
            onAltChange = { _, _ -> },
            onRetry = {},
        )
    }
}
