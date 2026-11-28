@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimeline
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineActor
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineDensity
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineEvent
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineLabels
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.dashboardgrid.MonarchDashboardGrid
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.widget.MonarchWidget
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.layout.sectioncard.MonarchSectionCard
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val GridSteps = 6
private const val TimelineSteps = 3
private const val SampleNow = 1_793_534_400_000L

/** The catalogue page of DashboardPersonalWork: a recipe of existing components. */
object DashboardPersonalWorkSection : CataloguePage {
    override val title = "DashboardPersonalWork"

    @Composable
    override fun Content() {
        val now = SampleNow
        var done by rememberSaveable { mutableStateOf(listOf<String>()) }
        val sections = listOf("Today", "Assigned to you", "Progress", "Recent activity")
        val tasks = listOf(
            Triple("t1", "Send the Q3 invoice to Acme", true),
            Triple("t2", "Review 4 new applicants", false),
        )
        val events = listOf(
            MonarchActivityTimelineEvent(
                id = "1",
                verb = "assigned you",
                time = now - 1_200_000L,
                actor = MonarchActivityTimelineActor("Ada Moss"),
                target = "Request 1042",
            ),
            MonarchActivityTimelineEvent(
                id = "2",
                verb = "commented on",
                time = now - 6_900_000L,
                actor = MonarchActivityTimelineActor("Ben Okafor"),
                target = "Onboarding plan",
            ),
        )
        CatalogueCaption("A person's own work: today, assigned, progress and activity")
        // The grid is the screen's scrolling list: one column on a phone, more from 1024dp.
        MonarchDashboardGrid(
            items = sections,
            key = { it },
            itemLabel = { it },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * GridSteps),
        ) { section ->
            when (section) {
                "Today" -> MonarchSectionCard(title = section, meta = "Wednesday, November 4") {
                    tasks.forEach { (id, title, overdue) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MonarchCheckboxRow(
                                checked = id in done,
                                onCheckedChange = { on -> done = if (on) done + id else done - id },
                                label = title,
                                modifier = Modifier.weight(1f),
                            )
                            if (overdue) MonarchBadge(text = "Overdue", tone = MonarchTone.Danger)
                        }
                    }
                }
                "Assigned to you" -> MonarchWidget(title = section, onViewAll = {}) {
                    listOf("Request 1042" to "In review", "Onboarding plan" to "To do").forEach { (title, status) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MonarchText(text = title, modifier = Modifier.weight(1f))
                            MonarchBadge(text = status)
                        }
                    }
                }
                "Progress" -> MonarchWidget(title = section, description = "Sprint 14") {
                    MonarchProgress(progress = 0.6f, label = "12 of 20 tasks done")
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
