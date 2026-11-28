package com.rookhq.monarch.components.charts.heatmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The Less to More legend of a heatmap: a swatch for each level, with a No
 * data swatch first when [showEmpty]. TalkBack reads it as one element: its
 * name and the range of every step, so the shades are not the only key.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HeatmapLegend(slot: MonarchChartColor, showEmpty: Boolean, words: HeatmapLegendWords) {
    val colors = MonarchTheme.colors
    val caption = MonarchTheme.typography.caption
    val levels: List<Int?> = (if (showEmpty) listOf(null) else emptyList<Int?>()) + (0..HeatmapLevels.TOP)
    FlowRow(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = (listOf(words.name) + words.steps).joinToString(", ")
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchHeatmapTokens.LegendGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = words.less, style = caption, color = MonarchHeatmapTokens.labelColor(colors))
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchHeatmapTokens.SwatchGap)) {
            levels.forEach { level ->
                Canvas(Modifier.size(MonarchHeatmapTokens.Swatch)) {
                    val radius = CornerRadius(MonarchHeatmapTokens.SwatchGap.toPx())
                    if (level == null) {
                        val stroke = MonarchHeatmapTokens.EmptyStroke.toPx()
                        drawRoundRect(
                            color = MonarchHeatmapTokens.swatchOutline(colors),
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = radius,
                            style = Stroke(stroke),
                        )
                    } else {
                        drawRoundRect(
                            color = MonarchHeatmapTokens.levelColor(level, slot, colors),
                            cornerRadius = radius,
                        )
                    }
                }
            }
        }
        Text(text = words.more, style = caption, color = MonarchHeatmapTokens.labelColor(colors))
    }
}
