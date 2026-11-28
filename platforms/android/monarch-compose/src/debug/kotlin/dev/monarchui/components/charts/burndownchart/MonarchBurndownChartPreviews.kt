package dev.monarchui.components.charts.burndownchart

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchBurndownChartPreview() {
    PreviewFrame {
        MonarchBurndownChart(
            title = "Sprint 14",
            points = listOf(
                MonarchBurndownChartPoint("Mon", remaining = 40.0, scope = 40.0),
                MonarchBurndownChartPoint("Tue", remaining = 38.0, scope = 40.0),
                MonarchBurndownChartPoint("Wed", remaining = 37.0, scope = 45.0),
                MonarchBurndownChartPoint("Thu", remaining = 31.0, scope = 43.0),
                MonarchBurndownChartPoint("Fri"),
                MonarchBurndownChartPoint("Mon"),
            ),
            activeIndex = 2,
            onActiveIndexChange = {},
            height = 180.dp,
            formatValue = { "${it.toInt()} pts" },
        )
        MonarchBurndownChart(
            title = "Sprint 13",
            points = listOf(30.0, 24.0, 18.0, 11.0, 3.0, 0.0).mapIndexed { i, left ->
                MonarchBurndownChartPoint("Day ${i + 1}", remaining = left)
            },
            height = 160.dp,
        )
    }
}
