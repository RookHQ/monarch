package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetric
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricChange
import com.rookhq.monarch.components.datadisplay.widget.MonarchWidget
import com.rookhq.monarch.components.datadisplay.widget.MonarchWidgetState
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Widget: one frame in each of its states. */
object MonarchWidgetSection : CataloguePage {
    override val title = "Widget"

    private val states = listOf("Ready", "Loading", "Empty", "Failed")
    private const val REVENUE = 128_400.0
    private const val CHANGE = 0.12

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        var retrying by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("Nothing yet") }
        CatalogueCaption("Choose a state")
        MonarchSegmentedControl {
            states.forEachIndexed { index, name ->
                Segment(
                    selected = selected == index,
                    onClick = {
                        selected = index
                        retrying = false
                    },
                    label = { Text(name) },
                )
            }
        }
        MonarchWidget(
            title = "Revenue",
            period = "Last 30 days",
            state = when (selected) {
                1 -> MonarchWidgetState.Loading
                2 -> MonarchWidgetState.Empty()
                3 -> MonarchWidgetState.Failed()
                else -> MonarchWidgetState.Ready
            },
            retrying = retrying,
            updatedAt = rememberFiveMinutesAgo(),
            footer = "Gross, before refunds",
            onRetry = { retrying = true },
            onViewAll = { last = "View all" },
            menu = { close ->
                listOf("Export", "Hide").forEach { action ->
                    MonarchMenuItem(
                        text = action,
                        onClick = {
                            last = action
                            close()
                        },
                    )
                }
            },
        ) {
            MonarchMetric(label = "Total", value = REVENUE, change = MonarchMetricChange(CHANGE, "vs last month"))
        }
        MonarchText(text = "Last action: $last", color = MonarchTheme.colors.textMuted)
    }

    @Composable
    private fun rememberFiveMinutesAgo(): Long =
        rememberSaveable { System.currentTimeMillis() - FIVE_MINUTES }

    private const val FIVE_MINUTES = 5 * 60_000L
}
