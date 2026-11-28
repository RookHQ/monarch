package dev.monarchui.components.geo.worldmap

import androidx.compose.runtime.Composable
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val previewData = listOf(
    MonarchWorldMapDatum("US", 1200.0, listOf(MonarchWorldMapDetail("Users", "340"))),
    MonarchWorldMapDatum("DE", 300.0),
    MonarchWorldMapDatum("IN", 150.0),
    MonarchWorldMapDatum("BR", 90.0),
    MonarchWorldMapDatum("SG", 40.0),
    MonarchWorldMapDatum("XX", 5.0),
)

@MonarchPreviews
@Composable
internal fun MonarchWorldMapPreview() {
    PreviewFrame {
        MonarchWorldMap(
            title = "Workspaces by country",
            data = previewData,
            description = "Last 30 days",
            extras = listOf(MonarchWorldMapExtra("Global", 75.0)),
        )
        MonarchWorldMap(
            title = "Workspaces by country",
            data = previewData,
            variant = MonarchWorldMapVariant.Bubble,
            color = MonarchChartColor.Data3,
        )
        MonarchWorldMap(title = "Workspaces", data = emptyList(), status = MonarchChartFrameStatus.Loading)
    }
}
