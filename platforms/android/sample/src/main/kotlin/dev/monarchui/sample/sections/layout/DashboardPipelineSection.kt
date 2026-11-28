@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.charts.barlist.MonarchBarList
import dev.monarchui.components.charts.barlist.MonarchBarListItem
import dev.monarchui.components.charts.barlist.MonarchBarListSort
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.funnelchart.MonarchFunnelChart
import dev.monarchui.components.charts.funnelchart.MonarchFunnelChartStage
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimeline
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineActor
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineDensity
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineEvent
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineLabels
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.dashboardgrid.MonarchDashboardGrid
import dev.monarchui.components.datadisplay.dashboardgrid.MonarchDashboardGridSpan
import dev.monarchui.components.datadisplay.insightstrip.MonarchInsight
import dev.monarchui.components.datadisplay.insightstrip.MonarchInsightOrigin
import dev.monarchui.components.datadisplay.insightstrip.MonarchInsightStrip
import dev.monarchui.components.datadisplay.leaderboard.MonarchLeaderboard
import dev.monarchui.components.datadisplay.leaderboard.MonarchLeaderboardEntry
import dev.monarchui.components.datadisplay.metric.MonarchMetricChange
import dev.monarchui.components.datadisplay.metric.MonarchMetricFormat
import dev.monarchui.components.datadisplay.statcard.MonarchStatCard
import dev.monarchui.components.datadisplay.widget.MonarchWidget
import dev.monarchui.components.datadisplay.widget.MonarchWidgetState
import dev.monarchui.components.scheduling.eventchip.MonarchEventChip
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val PipelineGridSteps = 7
private const val PipelineTimelineSteps = 3
private const val PipelineNow = 1_793_534_400_000L

/** The catalogue page of DashboardPipeline: a recipe of existing components, with the sections of the iOS recipe. */
object DashboardPipelineSection : CataloguePage {
    override val title = "DashboardPipeline"

    @Composable
    override fun Content() {
        val now = PipelineNow
        // The same sections as the web template and the iOS recipe, in the same order. The header parts (summary,
        // key figures, insights) are the first full-width items, because the grid is the screen's one scrolling list.
        val sections = listOf(
            "Summary", "Key figures", "Insights",
            "Pipeline", "Today", "Waiting on you", "Coming up", "Performance", "Top performers", "Recent activity",
        )
        val wide = setOf("Summary", "Key figures", "Insights")
        val events = listOf(
            MonarchActivityTimelineEvent(
                id = "1",
                verb = "moved",
                time = now - 1_800_000L,
                actor = MonarchActivityTimelineActor("Ana Lima"),
                target = "Globex",
            ),
            MonarchActivityTimelineEvent(
                id = "2",
                verb = "won",
                time = now - 9_000_000L,
                actor = MonarchActivityTimelineActor("Kai Chen"),
                target = "Initech",
                tone = MonarchTone.Success,
            ),
        )
        CatalogueCaption("The complete pipeline home screen: the sections of the web template and the iOS recipe")
        // One column on a phone, more from 1024dp. The title, period and primary action belong to the TopAppBar.
        MonarchDashboardGrid(
            items = sections,
            key = { it },
            itemLabel = { it },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PipelineGridSteps),
            span = {
                when (it) {
                    in wide -> MonarchDashboardGridSpan.Full
                    "Pipeline", "Performance" -> MonarchDashboardGridSpan.Two
                    else -> MonarchDashboardGridSpan.One
                }
            },
        ) { section -> PipelineSection(section, events, now) }
        CatalogueCaption("Loading, empty and failed sections")
        var retrying by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            // The funnel keeps its height while the stages load.
            MonarchFunnelChart(title = "Pipeline", stages = emptyList(), status = MonarchChartFrameStatus.Loading)
            MonarchWidget(title = "Today", state = MonarchWidgetState.Empty("Nothing scheduled today.")) {}
            MonarchWidget(title = "Top performers") {
                MonarchLeaderboard(
                    entries = emptyList(),
                    label = "Top performers",
                    errorMessage = "The ranking could not be loaded.",
                    retrying = retrying,
                    onRetry = { retrying = true },
                )
            }
        }
    }
}

/** One section of the home screen. */
@Composable
private fun PipelineSection(section: String, events: List<MonarchActivityTimelineEvent>, now: Long) {
    when (section) {
        "Summary" -> MonarchText(text = "Your deals this month", color = MonarchTheme.colors.textMuted)
        "Key figures" -> FlowRow(
            modifier = Modifier.semantics {
                isTraversalGroup = true
                contentDescription = section
            },
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
        ) {
            MonarchStatCard(
                label = "Pipeline value",
                value = 1_240_000.0,
                modifier = Modifier.weight(1f),
                format = MonarchMetricFormat.Currency("USD"),
                compact = true,
                change = MonarchMetricChange(value = 0.08, comparison = "vs last month"),
            )
            MonarchStatCard(
                label = "Open deals",
                value = 86.0,
                modifier = Modifier.weight(1f),
                change = MonarchMetricChange(value = 0.03),
            )
            MonarchStatCard(
                label = "Win rate",
                value = 0.27,
                modifier = Modifier.weight(1f),
                format = MonarchMetricFormat.Percent,
                change = MonarchMetricChange(value = -0.02),
            )
            MonarchStatCard(label = "Closing this month", value = 14.0, modifier = Modifier.weight(1f))
        }
        "Insights" -> MonarchInsightStrip(
            title = section,
            insights = listOf(
                MonarchInsight(
                    id = "stalled",
                    title = "6 deals have had no activity for 14 days",
                    why = "All six are in Proposal.",
                    origin = MonarchInsightOrigin.Rule,
                    tone = MonarchTone.Warning,
                    actionLabel = "Show deals",
                    onAction = {},
                ),
            ),
        )
        // The pipeline section is the funnel itself: its frame is already a card.
        "Pipeline" -> MonarchFunnelChart(
            title = section,
            description = "Deals by stage, this month",
            stages = listOf(
                MonarchFunnelChartStage("Lead", value = 240.0),
                MonarchFunnelChartStage("Qualified", value = 120.0),
                MonarchFunnelChartStage("Proposal", value = 48.0),
                MonarchFunnelChartStage("Won", value = 14.0),
            ),
        )
        "Today" -> MonarchWidget(title = section) {
            MonarchEventChip(
                title = "Acme renewal call",
                variant = MonarchEventChipVariant.Bar,
                time = "10:00 – 10:30",
                onClick = {},
            )
            MonarchEventChip(
                title = "Demo for Globex",
                variant = MonarchEventChipVariant.Bar,
                time = "14:00 – 15:00",
                onClick = {},
            )
        }
        "Waiting on you" -> MonarchWidget(title = section, onViewAll = {}) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonarchText(text = "Discount approval · Initech", modifier = Modifier.weight(1f))
                MonarchBadge(text = "Overdue", tone = MonarchTone.Danger)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonarchText(text = "Reply to Umbrella", modifier = Modifier.weight(1f))
                MonarchText(text = "Today", color = MonarchTheme.colors.textMuted)
            }
        }
        "Coming up" -> MonarchWidget(title = section) {
            listOf("Globex expected close" to "Fri", "Acme renewal" to "In 9 days").forEach { (title, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonarchText(text = title, modifier = Modifier.weight(1f))
                    MonarchText(text = value, color = MonarchTheme.colors.textMuted)
                }
            }
        }
        "Performance" -> MonarchWidget(title = section, period = "This month") {
            MonarchBarList(
                items = listOf(
                    MonarchBarListItem("Referral", value = 6.0),
                    MonarchBarListItem("Website", value = 5.0),
                    MonarchBarListItem("Outbound", value = 3.0),
                ),
                label = "Won deals by source",
            )
        }
        "Top performers" -> MonarchWidget(title = section) {
            MonarchLeaderboard(
                entries = listOf(
                    MonarchLeaderboardEntry("Ana Lima", value = 182_000.0, display = "$182K", change = 0.12),
                    MonarchLeaderboardEntry("Sam Ortiz", value = 140_000.0, display = "$140K"),
                    MonarchLeaderboardEntry("Kai Chen", value = 96_000.0, display = "$96K", change = -0.05),
                ),
                sort = MonarchBarListSort.Descending,
                label = section,
                valueLabel = "won",
                changeComparison = "vs last month",
                showAvatars = false,
            )
        }
        else -> MonarchWidget(title = section, onViewAll = {}) {
            MonarchActivityTimeline(
                events = events,
                // A lazy list inside a grid item needs a bounded height.
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = MonarchTheme.spacing.space24 * PipelineTimelineSteps),
                groupByDay = false,
                density = MonarchActivityTimelineDensity.Compact,
                now = now,
                timeZone = "UTC",
                labels = MonarchActivityTimelineLabels(list = "Recent activity", empty = "No activity yet"),
            )
        }
    }
}
