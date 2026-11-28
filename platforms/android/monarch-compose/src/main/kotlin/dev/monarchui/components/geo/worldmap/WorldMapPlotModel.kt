package dev.monarchui.components.geo.worldmap

import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.heatmap.HeatmapScale

/** What the plot draws: the values, the variant, the colour and the inspected country. */
internal class WorldMapPlotModel(
    val byCode: Map<String, MonarchWorldMapDatum>,
    val placed: List<MonarchWorldMapDatum>,
    val variant: MonarchWorldMapVariant,
    val slot: MonarchChartColor,
    val scale: HeatmapScale,
    val maximum: Double,
    val active: String?,
)
