package dev.monarchui.sample.sections.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicator
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorDirection
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorGoodDirection
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorSize
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of DeltaIndicator. */
object MonarchDeltaIndicatorSection : CataloguePage {
    override val title = "DeltaIndicator"

    private val changes = listOf(0.12, -0.08, 0.0, null)

    @Composable
    override fun Content() {
        var index by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Plain, up is better")
        MonarchDeltaIndicator(value = changes[index], comparison = "vs last month")
        MonarchButton(text = "Next value", onClick = { index = (index + 1) % changes.size })
        CatalogueCaption("Down is better: latency")
        MonarchDeltaIndicator(
            value = -42.0,
            comparison = "vs yesterday",
            goodDirection = MonarchDeltaIndicatorGoodDirection.Down,
            formatValue = { "${it.toInt()} ms" },
        )
        CatalogueCaption("Subtle, medium, with an icon")
        MonarchDeltaIndicator(
            current = 120.0,
            previous = 100.0,
            comparison = "vs last week",
            variant = MonarchDeltaIndicatorVariant.Subtle,
            size = MonarchDeltaIndicatorSize.Md,
            icon = { direction ->
                if (direction != MonarchDeltaIndicatorDirection.Flat) {
                    MonarchIcon(imageVector = SampleIcons.ArrowUp, contentDescription = null, size = MonarchIconSize.Sm)
                }
            },
        )
    }
}
