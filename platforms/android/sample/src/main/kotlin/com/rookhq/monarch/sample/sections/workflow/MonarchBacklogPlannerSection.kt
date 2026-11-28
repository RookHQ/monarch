@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.backlogplanner.MonarchBacklogBucket
import com.rookhq.monarch.components.workflow.backlogplanner.MonarchBacklogPlanner
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of BacklogPlanner: long press a row to drag it, or use its Move button. */
object MonarchBacklogPlannerSection : CataloguePage {
    override val title = "BacklogPlanner"

    private const val SAVE_MILLIS = 400L
    private const val HEIGHT_STEPS = 6
    private const val SPRINT_CAPACITY = 8

    private class Issue(val key: String, val title: String, val points: Int?)

    private fun start(): List<MonarchBacklogBucket<Issue>> = listOf(
        MonarchBacklogBucket(
            id = "sprint-14",
            title = "Sprint 14",
            items = listOf(
                Issue("ABC-12", "Fix login on slow networks", points = 3),
                Issue("ABC-15", "Export to CSV", points = 2),
            ),
            subtitle = "Nov 10 – Nov 21",
            capacity = SPRINT_CAPACITY,
            badge = { MonarchBadge(text = "Active", tone = MonarchTone.Success) },
        ),
        MonarchBacklogBucket(
            id = "sprint-15",
            title = "Sprint 15",
            items = emptyList(),
            subtitle = "Nov 24 – Dec 5",
            capacity = SPRINT_CAPACITY,
        ),
        MonarchBacklogBucket(
            id = "backlog",
            title = "Backlog",
            items = listOf(
                Issue("ABC-18", "Billing page", points = 5),
                Issue("ABC-21", "Search filters", points = null),
                Issue("ABC-22", "Dark mode for charts", points = 1),
            ),
        ),
    )

    @Composable
    override fun Content() {
        var buckets by remember { mutableStateOf(start()) }
        CatalogueCaption("Sprints and the backlog; saving takes a moment")
        MonarchBacklogPlanner(
            buckets = buckets,
            key = { it.key },
            itemLabel = { it.key },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            itemPoints = { it.points },
            onMove = { _, next ->
                buckets = next
                delay(SAVE_MILLIS)
            },
        ) { issue, _ ->
            MonarchText(text = issue.key, style = MonarchTextStyle.Code)
            MonarchText(text = issue.title)
        }
    }
}
