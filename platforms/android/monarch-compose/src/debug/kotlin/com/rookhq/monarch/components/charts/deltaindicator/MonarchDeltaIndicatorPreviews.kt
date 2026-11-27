package com.rookhq.monarch.components.charts.deltaindicator

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchDeltaIndicatorPlainPreview() {
    PreviewFrame {
        MonarchDeltaIndicator(value = 0.12, comparison = "vs last month")
        MonarchDeltaIndicator(value = -0.08, comparison = "vs last month")
        MonarchDeltaIndicator(value = -0.08, goodDirection = MonarchDeltaIndicatorGoodDirection.Down)
        MonarchDeltaIndicator(value = 0.3, goodDirection = MonarchDeltaIndicatorGoodDirection.Neither)
        MonarchDeltaIndicator(value = 0.0, comparison = "vs last month")
        MonarchDeltaIndicator(value = null, comparison = "vs last month")
    }
}

@MonarchPreviews
@Composable
internal fun MonarchDeltaIndicatorSubtlePreview() {
    PreviewFrame {
        MonarchDeltaIndicatorSize.entries.forEach { size ->
            MonarchDeltaIndicator(
                value = 0.12,
                comparison = "vs last week",
                variant = MonarchDeltaIndicatorVariant.Subtle,
                size = size,
                icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        }
        MonarchDeltaIndicator(value = -42.0, variant = MonarchDeltaIndicatorVariant.Subtle, formatValue = { "$it ms" })
    }
}
