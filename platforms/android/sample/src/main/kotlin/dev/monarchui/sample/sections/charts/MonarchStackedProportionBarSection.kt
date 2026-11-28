package dev.monarchui.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.charts.stackedproportionbar.MonarchStackedProportionBar
import dev.monarchui.components.charts.stackedproportionbar.MonarchStackedProportionBarLegendContent
import dev.monarchui.components.charts.stackedproportionbar.MonarchStackedProportionBarSegment
import dev.monarchui.components.charts.stackedproportionbar.MonarchStackedProportionBarSize
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of StackedProportionBar. */
object MonarchStackedProportionBarSection : CataloguePage {
    override val title = "StackedProportionBar"

    private val pipeline = listOf(
        MonarchStackedProportionBarSegment("new", "New", 1240.0),
        MonarchStackedProportionBarSegment("campaign", "In a campaign", 610.0),
        MonarchStackedProportionBarSegment("replied", "Replied", 182.0),
        MonarchStackedProportionBarSegment("converted", "Converted", 41.0),
    )
    private val storage = listOf(
        MonarchStackedProportionBarSegment("media", "Media", 42.0),
        MonarchStackedProportionBarSegment("docs", "Documents", 18.0),
    )
    private val monitors = listOf(
        MonarchStackedProportionBarSegment("up", "Operational", 41.0, MonarchTone.Success),
        MonarchStackedProportionBarSegment("slow", "Degraded", 3.0, MonarchTone.Warning),
        MonarchStackedProportionBarSegment("down", "Down", 1.0, MonarchTone.Danger),
    )
    private const val CAPACITY = 100.0

    @Composable
    override fun Content() {
        var content by rememberSaveable { mutableIntStateOf(0) }
        var size by rememberSaveable { mutableIntStateOf(1) }
        CatalogueCaption("Legend content")
        Choice(MonarchStackedProportionBarLegendContent.entries.map { it.name }, content) { content = it }
        CatalogueCaption("Size")
        Choice(MonarchStackedProportionBarSize.entries.map { it.name }, size) { size = it }
        MonarchStackedProportionBar(
            label = "Lead pipeline",
            segments = pipeline,
            legendContent = MonarchStackedProportionBarLegendContent.entries[content],
            size = MonarchStackedProportionBarSize.entries[size],
        )
        CatalogueCaption("Capacity with a remainder")
        MonarchStackedProportionBar(
            label = "Storage",
            segments = storage,
            total = CAPACITY,
            remainderLabel = "Free",
            legendContent = MonarchStackedProportionBarLegendContent.Value,
            formatValue = { "${it.toInt()} GB" },
        )
        CatalogueCaption("Status mode")
        MonarchStackedProportionBar(label = "Monitors", segments = monitors, size = MonarchStackedProportionBarSize.Sm)
        CatalogueCaption("No legend, and no data")
        MonarchStackedProportionBar(label = "Lead pipeline", segments = pipeline, showLegend = false)
        MonarchStackedProportionBar(label = "Lead pipeline", segments = emptyList())
    }

    @Composable
    private fun Choice(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        MonarchSegmentedControl {
            options.forEachIndexed { index, option ->
                Segment(
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    label = { Text(option) },
                )
            }
        }
    }
}
