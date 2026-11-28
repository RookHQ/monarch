package com.rookhq.monarch.sample.sections.geo

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMap
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapDatum
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapDetail
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapExtra
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of WorldMap. */
object MonarchWorldMapSection : CataloguePage {
    override val title = "WorldMap"

    private val workspaces = listOf(
        MonarchWorldMapDatum("US", 1200.0, listOf(MonarchWorldMapDetail("Users", "3,400"))),
        MonarchWorldMapDatum("DE", 300.0, listOf(MonarchWorldMapDetail("Users", "910"))),
        MonarchWorldMapDatum("IN", 150.0),
        MonarchWorldMapDatum("BR", 90.0),
        MonarchWorldMapDatum("GB", 240.0),
        MonarchWorldMapDatum("JP", 110.0),
        MonarchWorldMapDatum("AU", 60.0),
        MonarchWorldMapDatum("SG", 40.0),
        MonarchWorldMapDatum("MT", 12.0),
    )

    private val extras = listOf(MonarchWorldMapExtra("Global", 75.0), MonarchWorldMapExtra("EU", 420.0))

    private val signups = listOf(MonarchWorldMapDatum("FR", 80.0), MonarchWorldMapDatum("ZZ", 4.0))

    @Composable
    override fun Content() {
        var variant by rememberSaveable { mutableStateOf(MonarchWorldMapVariant.Choropleth) }
        var chosen by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Choropleth or bubbles; tap a country")
        MonarchSegmentedControl {
            MonarchWorldMapVariant.entries.forEachIndexed { index, option ->
                Segment(
                    selected = variant == option,
                    onClick = { variant = option },
                ) { Text(option.name) }
            }
        }
        MonarchWorldMap(
            title = "Workspaces by country",
            data = workspaces,
            description = "Last 30 days",
            variant = variant,
            extras = extras,
            footer = "Source: billing addresses.",
            onSelect = { chosen = it.country },
        )
        MonarchText(text = "Selected: ${chosen ?: "none"}")
        CatalogueCaption("Another color, a code that is not on the map")
        MonarchWorldMap(
            title = "Signups",
            data = signups,
            color = MonarchChartColor.Data3,
        )
        CatalogueCaption("Loading")
        MonarchWorldMap(title = "Signups", data = emptyList(), status = MonarchChartFrameStatus.Loading)
    }
}
