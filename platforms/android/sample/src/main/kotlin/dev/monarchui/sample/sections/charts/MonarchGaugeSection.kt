package dev.monarchui.sample.sections.charts

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.charts.gauge.MonarchGauge
import dev.monarchui.components.charts.gauge.MonarchGaugeBand
import dev.monarchui.components.charts.gauge.MonarchGaugeIndicator
import dev.monarchui.components.charts.gauge.MonarchGaugeSize
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Gauge. */
object MonarchGaugeSection : CataloguePage {
    override val title = "Gauge"

    private const val START_VALUE = 72f
    private val scale = 0f..100f
    private val health = listOf(
        MonarchGaugeBand(0.0..70.0, MonarchTone.Success, "Healthy"),
        MonarchGaugeBand(70.0..90.0, MonarchTone.Warning, "At risk"),
        MonarchGaugeBand(90.0..100.0, MonarchTone.Danger, "Critical"),
    )

    @Composable
    override fun Content() {
        var value by rememberSaveable { mutableFloatStateOf(START_VALUE) }
        Slider(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.semantics { contentDescription = "CPU" },
            valueRange = scale,
        )
        CatalogueCaption("Arc with bands and a target")
        MonarchGauge(
            value = value.toDouble(),
            bands = health,
            target = 80.0,
            label = "CPU",
            description = "Last hour",
        )
        CatalogueCaption("Needle")
        MonarchGauge(
            value = value.toDouble(),
            bands = health,
            indicator = MonarchGaugeIndicator.Needle,
            label = "Score",
        )
        CatalogueCaption("Small, no value")
        MonarchGauge(value = null, size = MonarchGaugeSize.Sm, label = "Memory")
    }
}
