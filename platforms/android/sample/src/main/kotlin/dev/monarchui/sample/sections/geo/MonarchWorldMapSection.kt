package dev.monarchui.sample.sections.geo

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.geo.worldmap.MonarchWorldMap
import dev.monarchui.components.geo.worldmap.MonarchWorldMapDatum
import dev.monarchui.components.geo.worldmap.MonarchWorldMapDetail
import dev.monarchui.components.geo.worldmap.MonarchWorldMapExtra
import dev.monarchui.components.geo.worldmap.MonarchWorldMapVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

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
