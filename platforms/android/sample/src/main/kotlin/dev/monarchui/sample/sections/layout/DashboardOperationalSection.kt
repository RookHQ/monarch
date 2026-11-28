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
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorGoodDirection
import dev.monarchui.components.charts.linechart.MonarchLineChart
import dev.monarchui.components.charts.linechart.MonarchLineChartSeries
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimeline
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineActor
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineActorKind
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
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.datadisplay.widget.MonarchWidget
import dev.monarchui.components.datadisplay.widget.MonarchWidgetState
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val OperationalGridSteps = 7
private const val OperationalTimelineSteps = 3
private const val OperationalNow = 1_793_534_400_000L
private val OperationalOpened = listOf(42.0, 38.0, 51.0, 45.0, 37.0)

/** The catalogue page of DashboardOperational: a recipe of existing components, with the sections of the iOS recipe. */
object DashboardOperationalSection : CataloguePage {
    override val title = "DashboardOperational"

    @Composable
    override fun Content() {
        val now = OperationalNow
        // The same sections as the web template and the iOS recipe, in the same order. The header parts (summary,
        // key figures, insights) are the first full-width items, because the grid is the screen's one scrolling list.
        val sections = listOf(
            "Summary", "Key figures", "Insights",
            "Status", "Needs attention", "My queue", "Trend", "Team load", "Coming up", "Recent activity",
        )
        val wide = setOf("Summary", "Key figures", "Insights", "Status")
        val events = listOf(
            MonarchActivityTimelineEvent(
                id = "1",
                verb = "resolved",
                time = now - 1_200_000L,
                actor = MonarchActivityTimelineActor("Ada Moss"),
                target = "Incident 311",
            ),
            MonarchActivityTimelineEvent(
                id = "2",
                verb = "escalated",
                time = now - 5_400_000L,
                actor = MonarchActivityTimelineActor("SLA rule", kind = MonarchActivityTimelineActorKind.Automation),
                target = "Ticket 4410",
                tone = MonarchTone.Warning,
            ),
        )
        CatalogueCaption("The complete operational home screen: the sections of the web template and the iOS recipe")
        // One column on a phone, more from 1024dp. The title, scope, period and actions belong to the TopAppBar.
        MonarchDashboardGrid(
            items = sections,
            key = { it },
            itemLabel = { it },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * OperationalGridSteps),
            span = {
                when (it) {
                    in wide -> MonarchDashboardGridSpan.Full
                    "Needs attention", "Trend" -> MonarchDashboardGridSpan.Two
                    else -> MonarchDashboardGridSpan.One
                }
            },
        ) { section -> OperationalSection(section, events, now) }
        CatalogueCaption("Loading, empty and failed sections")
        var retrying by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            // Placeholder figures keep their size while the first load runs.
            FlowRow(
                modifier = Modifier.semantics {
                    isTraversalGroup = true
                    contentDescription = "Key figures"
                },
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            ) {
                MonarchStatCard(label = "Open tickets", value = null, modifier = Modifier.weight(1f), loading = true)
                MonarchStatCard(label = "First reply", value = null, modifier = Modifier.weight(1f), loading = true)
            }
            // Each section loads and fails on its own; the rest of the screen stays usable.
            MonarchWidget(
                title = "Needs attention",
                state = MonarchWidgetState.Empty("Nothing needs attention right now."),
            ) {}
            MonarchWidget(
                title = "Team load",
                state = MonarchWidgetState.Failed("Team load could not be loaded."),
                retrying = retrying,
                onRetry = { retrying = true },
            ) {}
            MonarchWidget(title = "Recent activity", state = MonarchWidgetState.Loading) {}
        }
    }
}

/** One section of the home screen. */
@Composable
private fun OperationalSection(section: String, events: List<MonarchActivityTimelineEvent>, now: Long) {
    when (section) {
        "Summary" -> Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            MonarchText(text = "All teams, last 7 days", color = MonarchTheme.colors.textMuted)
            MonarchBanner(
                message = "Connect your status page to see live service health.",
                action = { MonarchButton(text = "Connect", onClick = {}, size = MonarchButtonSize.Sm) },
            )
        }
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
                label = "Open tickets",
                value = 128.0,
                modifier = Modifier.weight(1f),
                change = MonarchMetricChange(
                    value = 0.12,
                    comparison = "vs last week",
                    goodDirection = MonarchDeltaIndicatorGoodDirection.Down,
                ),
            )
            MonarchStatCard(
                label = "First reply",
                text = "1h 12m",
                modifier = Modifier.weight(1f),
                change = MonarchMetricChange(
                    value = -0.08,
                    goodDirection = MonarchDeltaIndicatorGoodDirection.Down,
                ),
            )
            MonarchStatCard(
                label = "Solved",
                value = 342.0,
                modifier = Modifier.weight(1f),
                change = MonarchMetricChange(value = 0.05),
            )
            MonarchStatCard(
                label = "Satisfaction",
                value = 0.94,
                modifier = Modifier.weight(1f),
                format = MonarchMetricFormat.Percent,
            )
        }
        "Insights" -> MonarchInsightStrip(
            title = section,
            insights = listOf(
                MonarchInsight(
                    id = "breach",
                    title = "4 tickets breach their SLA within an hour",
                    why = "All four are waiting on tier 2.",
                    origin = MonarchInsightOrigin.Rule,
                    tone = MonarchTone.Warning,
                    actionLabel = "Open tickets",
                    onAction = {},
                ),
            ),
        )
        "Status" -> MonarchWidget(title = section) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchStatusBadge(MonarchStatusDefinition("API: operational", MonarchTone.Success))
                MonarchStatusBadge(MonarchStatusDefinition("Email: degraded", MonarchTone.Warning))
                MonarchStatusBadge(MonarchStatusDefinition("Chat: operational", MonarchTone.Success))
            }
        }
        "Needs attention" -> MonarchWidget(title = section, onViewAll = {}) {
            listOf(
                Triple("Request 1042 · Billing", "Breaches in 20 min", MonarchTone.Danger),
                Triple("Incident 88 · Email", "Open", MonarchTone.Warning),
            ).forEach { (title, status, tone) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonarchText(text = title, modifier = Modifier.weight(1f))
                    MonarchBadge(text = status, tone = tone)
                }
            }
        }
        "My queue" -> MonarchWidget(title = section, onViewAll = {}) {
            listOf("Request 1039" to "Due today", "Request 1027" to "Due tomorrow").forEach { (title, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonarchText(text = title, modifier = Modifier.weight(1f))
                    MonarchText(text = value, color = MonarchTheme.colors.textMuted)
                }
            }
        }
        "Coming up" -> MonarchWidget(title = section) {
            listOf("Billing review" to "Thu", "Quarterly SLA report" to "Mon").forEach { (title, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonarchText(text = title, modifier = Modifier.weight(1f))
                    MonarchText(text = value, color = MonarchTheme.colors.textMuted)
                }
            }
        }
        // A chart section is the chart itself: its frame is already a card.
        "Trend" -> MonarchLineChart(
            title = "Tickets opened",
            categories = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
            series = listOf(
                MonarchLineChartSeries("opened", "Opened", values = OperationalOpened),
            ),
        )
        "Team load" -> MonarchWidget(title = section) {
            MonarchLeaderboard(
                entries = listOf(
                    MonarchLeaderboardEntry("Ada Moss", value = 14.0),
                    MonarchLeaderboardEntry("Ben Okafor", value = 11.0),
                    MonarchLeaderboardEntry("Cara Lind", value = 6.0),
                ),
                label = "Open tickets per agent",
                showAvatars = false,
            )
        }
        else -> MonarchWidget(title = section, onViewAll = {}) {
            MonarchActivityTimeline(
                events = events,
                // A lazy list inside a grid item needs a bounded height.
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = MonarchTheme.spacing.space24 * OperationalTimelineSteps),
                groupByDay = false,
                density = MonarchActivityTimelineDensity.Compact,
                now = now,
                timeZone = "UTC",
                labels = MonarchActivityTimelineLabels(list = "Recent activity", empty = "No activity yet"),
            )
        }
    }
}
