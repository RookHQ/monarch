package com.rookhq.monarch.components.intelligence.airesponseskeleton

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiResponseSkeletonPreview() {
    PreviewFrame {
        MonarchAiResponseSkeleton()
        MonarchAiResponseSkeleton(shape = MonarchAiResponseSkeletonShape.Cards, lines = 2)
        MonarchAiResponseSkeleton(shape = MonarchAiResponseSkeletonShape.Table, lines = 3)
    }
}
