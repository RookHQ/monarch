package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorGoodDirection
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetric
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricChange
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricFormat
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricLayout
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricSize
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of Metric. The slider stands in for a value that changes. */
object MonarchMetricSection : CataloguePage {
    override val title = "Metric"

    private const val PREVIOUS_LATENCY = 900.0
    private const val START_LATENCY = 840f
    private const val MAX_LATENCY = 2000f
    private const val REVENUE = 3_400_000.0
    private const val CONVERSION = 0.12
    private const val OPEN = 128.0

    @Composable
    override fun Content() {
        var latency by rememberSaveable { mutableFloatStateOf(START_LATENCY) }
        var compact by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("A headline figure with a change; lower is better")
        MonarchMetric(
            label = "Latency",
            value = latency.toDouble(),
            unit = "ms",
            change = MonarchMetricChange.between(
                current = latency.toDouble(),
                previous = PREVIOUS_LATENCY,
                comparison = "vs last week",
                goodDirection = MonarchDeltaIndicatorGoodDirection.Down,
            ),
            size = MonarchMetricSize.Lg,
        )
        Slider(
            value = latency,
            onValueChange = { latency = it },
            modifier = Modifier.semantics { contentDescription = "Latency" },
            valueRange = 0f..MAX_LATENCY,
        )
        CatalogueCaption("Currency, compact")
        MonarchSwitchRow(checked = compact, onCheckedChange = { compact = it }, label = "Compact")
        MonarchMetric(
            label = "Revenue",
            value = REVENUE,
            format = MonarchMetricFormat.Currency("EUR"),
            compact = compact,
        )
        CatalogueCaption("Percent, inline and end aligned")
        MonarchMetric(
            label = "Conversion",
            value = CONVERSION,
            format = MonarchMetricFormat.Percent,
            layout = MonarchMetricLayout.Inline,
        )
        MonarchMetric(label = "Open tickets", value = OPEN, size = MonarchMetricSize.Sm, alignment = Alignment.End)
        CatalogueCaption("No data")
        MonarchMetric(label = "Errors", value = null, unit = "per hour")
    }
}
