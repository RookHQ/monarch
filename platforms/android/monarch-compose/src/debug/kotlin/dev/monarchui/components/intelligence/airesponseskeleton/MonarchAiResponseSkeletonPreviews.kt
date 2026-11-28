package dev.monarchui.components.intelligence.airesponseskeleton

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiResponseSkeletonPreview() {
    PreviewFrame {
        MonarchAiResponseSkeleton()
        MonarchAiResponseSkeleton(shape = MonarchAiResponseSkeletonShape.Cards, lines = 2)
        MonarchAiResponseSkeleton(shape = MonarchAiResponseSkeletonShape.Table, lines = 3)
    }
}
