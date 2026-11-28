package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorGoodDirection
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricChange
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricFormat
import com.rookhq.monarch.components.datadisplay.statcard.MonarchStatCard
import com.rookhq.monarch.components.datadisplay.statcard.MonarchStatCardSize
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetryVariant
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of KPIRow: MonarchStatCard in two columns, or in a row that scrolls sideways. */
object KpiRowSection : CataloguePage {
    override val title = "KPIRow"

    private val states = listOf("Ready", "Loading", "Error")
    private const val PLACEHOLDERS = 4
    private const val PEEK = 0.7f
    private val Euro = MonarchMetricFormat.Currency("EUR")

    private class Figure(val label: String, val value: Double, val previous: Double)

    private val figures = listOf(
        Figure("Revenue", 48_200.0, 43_000.0),
        Figure("New customers", 128.0, 140.0),
        Figure("Orders", 1_204.0, 1_100.0),
        Figure("Open tickets", 37.0, 41.0),
    )

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Choose a state")
        MonarchSegmentedControl {
            states.forEachIndexed { index, name ->
                Segment(
                    selected = selected == index,
                    onClick = { selected = index },
                    label = { Text(name) },
                )
            }
        }
        CatalogueCaption("Two columns")
        if (selected == 2) {
            MonarchInlineErrorRetry(
                message = "The figures could not be loaded.",
                onRetry = { selected = 0 },
                variant = MonarchInlineErrorRetryVariant.Banner,
            )
        } else {
            FlowRow(
                modifier = Modifier.semantics {
                    isTraversalGroup = true
                    contentDescription = "This month"
                },
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            ) {
                if (selected == 1) {
                    repeat(PLACEHOLDERS) {
                        MonarchStatCard(label = "Loading", value = null, modifier = Modifier.weight(1f), loading = true)
                    }
                } else {
                    figures.forEach { figure ->
                        MonarchStatCard(
                            label = figure.label,
                            value = figure.value,
                            modifier = Modifier.weight(1f),
                            change = MonarchMetricChange.between(
                                figure.value,
                                figure.previous,
                                goodDirection = if (figure.label == "Open tickets") {
                                    MonarchDeltaIndicatorGoodDirection.Down
                                } else {
                                    MonarchDeltaIndicatorGoodDirection.Up
                                },
                            ),
                        )
                    }
                }
            }
        }
        CatalogueCaption("A hero row that scrolls sideways")
        val state = rememberLazyListState()
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
        ) {
            items(figures, key = { it.label }) { figure ->
                MonarchStatCard(
                    label = figure.label,
                    value = figure.value,
                    modifier = Modifier.fillParentMaxWidth(PEEK),
                    format = if (figure.label == "Revenue") Euro else MonarchMetricFormat.Number,
                    size = MonarchStatCardSize.Lg,
                    loading = selected == 1,
                )
            }
        }
    }
}
