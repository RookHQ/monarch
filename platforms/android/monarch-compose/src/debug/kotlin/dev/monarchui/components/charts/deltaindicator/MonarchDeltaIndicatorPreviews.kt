package dev.monarchui.components.charts.deltaindicator

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
