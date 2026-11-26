@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.charts.barlist.MonarchBarList
import com.rookhq.monarch.components.charts.barlist.MonarchBarListItem
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChart
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartSegment
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimeline
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineActor
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineActorKind
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineDensity
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineEvent
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineLabels
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.dashboardgrid.MonarchDashboardGrid
import com.rookhq.monarch.components.datadisplay.widget.MonarchWidget
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

private const val GridSteps = 6
private const val TimelineSteps = 3
private const val SampleNow = 1_793_534_400_000L

/** The catalogue page of DashboardPortfolio: a recipe of existing components. */
object DashboardPortfolioSection : CataloguePage {
    override val title = "DashboardPortfolio"

    @Composable
    override fun Content() {
        val now = SampleNow
        val sections = listOf("Portfolio by type", "Needs attention", "Largest contracts", "Recent activity")
        val events = listOf(
            MonarchActivityTimelineEvent(
                id = "1",
                verb = "renewed",
                time = now - 3_600_000L,
                actor = MonarchActivityTimelineActor("Ada Moss"),
                target = "Contract 88",
            ),
            MonarchActivityTimelineEvent(
                id = "2",
                verb = "flagged",
                time = now - 7_200_000L,
                actor = MonarchActivityTimelineActor(
                    "Renewal rule",
                    kind = MonarchActivityTimelineActorKind.Automation,
                ),
                target = "Trademark EU-4471",
                tone = MonarchTone.Warning,
            ),
        )
        CatalogueCaption("A portfolio home: composition, attention, ranking and activity")
        // The grid is the screen's scrolling list: one column on a phone, more from 1024dp.
        MonarchDashboardGrid(
            items = sections,
            key = { it },
            itemLabel = { it },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * GridSteps),
        ) { section ->
            when (section) {
                "Portfolio by type" -> MonarchDonutChart(
                    title = section,
                    segments = listOf(
                        MonarchDonutChartSegment("patents", "Patents", value = 42.0),
                        MonarchDonutChartSegment("trademarks", "Trademarks", value = 31.0),
                        MonarchDonutChartSegment("designs", "Designs", value = 12.0),
                    ),
                    centerLabel = "assets",
                )
                "Needs attention" -> MonarchWidget(title = section, onViewAll = {}) {
                    listOf(
                        Triple("Trademark EU-4471", "Expires in 12 days", MonarchTone.Warning),
                        Triple("Patent US-2093", "Fee overdue", MonarchTone.Danger),
                    ).forEach { (title, status, tone) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MonarchText(text = title, modifier = Modifier.weight(1f))
                            MonarchBadge(text = status, tone = tone)
                        }
                    }
                }
                "Largest contracts" -> MonarchWidget(title = section) {
                    MonarchBarList(
                        items = listOf(
                            MonarchBarListItem("Acme Ltd", value = 1_200_000.0, displayValue = "€1.2M"),
                            MonarchBarListItem("Globex", value = 840_000.0, displayValue = "€840K"),
                            MonarchBarListItem("Initech", value = 310_000.0, displayValue = "€310K"),
                        ),
                        label = "Largest contracts by value",
                    )
                }
                else -> MonarchWidget(title = section, onViewAll = {}) {
                    MonarchActivityTimeline(
                        events = events,
                        // A lazy list inside a grid item needs a bounded height.
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = MonarchTheme.spacing.space24 * TimelineSteps),
                        groupByDay = false,
                        density = MonarchActivityTimelineDensity.Compact,
                        now = now,
                        timeZone = "UTC",
                        labels = MonarchActivityTimelineLabels(list = "Recent activity", empty = "No activity yet"),
                    )
                }
            }
        }
    }
}
