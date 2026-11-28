@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimeline
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineDependency
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineDependencyKind
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineGroup
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineItem
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineMilestone
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineView
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineZoom
import com.rookhq.monarch.components.workflow.gantttimeline.rememberMonarchGanttTimelineState
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** Monday 5 January 2026, 00:00 UTC. */
private const val GanttJan5 = 1_767_571_200_000L
private const val GanttDay = 86_400_000L

private const val GanttFirstDay = 5

private fun ganttJan(day: Int): Long = GanttJan5 + (day - GanttFirstDay) * GanttDay

private val GanttItems = listOf(
    MonarchGanttTimelineItem(
        "brief",
        "Write the brief",
        start = ganttJan(5),
        end = ganttJan(9),
        progress = 1f,
        tone = MonarchTone.Success,
        groupId = "web",
    ),
    MonarchGanttTimelineItem(
        "design",
        "Design",
        start = ganttJan(12),
        end = ganttJan(23),
        progress = 0.6f,
        groupId = "web",
    ),
    MonarchGanttTimelineItem(
        "build",
        "Build",
        start = ganttJan(21),
        end = ganttJan(44),
        conflict = "Starts before Design ends",
        groupId = "web",
    ),
    MonarchGanttTimelineItem("api", "API changes", start = ganttJan(26), end = ganttJan(35), parentId = "build"),
    MonarchGanttTimelineItem("ui", "Screens", start = ganttJan(33), end = ganttJan(44), parentId = "build"),
    MonarchGanttTimelineItem("launch", "Launch", end = ganttJan(51), meta = "Fixed date", editable = false),
    MonarchGanttTimelineItem("research", "Pricing research"),
)

private val GanttToday = ganttJan(14)

private val GanttGroups = listOf(MonarchGanttTimelineGroup("web", "Website relaunch", meta = "3 items"))

private val GanttLinks = listOf(
    MonarchGanttTimelineDependency("l1", "brief", "design"),
    MonarchGanttTimelineDependency("l2", "design", "build", conflict = true),
    MonarchGanttTimelineDependency("l3", "api", "ui", MonarchGanttTimelineDependencyKind.StartToStart),
)

private val GanttMilestones = listOf(
    MonarchGanttTimelineMilestone("m1", "Design review", ganttJan(23)),
    MonarchGanttTimelineMilestone("m2", "Beta", ganttJan(40), itemId = "build"),
)

/** The catalogue page of GanttTimeline. */
object MonarchGanttTimelineSection : CataloguePage {
    override val title = "GanttTimeline"

    @Composable
    override fun Content() {
        var items by remember { mutableStateOf(GanttItems) }
        CatalogueCaption("Editable, with groups, sub-tasks, dependencies and milestones")
        MonarchGanttTimeline(
            items = items,
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {
                MonarchIcon(
                    SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
            state = rememberMonarchGanttTimelineState(initialStart = GanttJan5),
            groups = GanttGroups,
            dependencies = GanttLinks,
            milestones = GanttMilestones,
            today = GanttToday,
            onChange = { item, dates ->
                items = items.map {
                    if (it.id != item.id) {
                        it
                    } else {
                        MonarchGanttTimelineItem(
                            id = it.id,
                            label = it.label,
                            start = dates.start,
                            end = dates.end,
                            parentId = it.parentId,
                            groupId = it.groupId,
                            progress = it.progress,
                            tone = it.tone,
                            editable = it.editable,
                            conflict = it.conflict,
                            meta = it.meta,
                        )
                    }
                }
            },
            onOpen = {},
        )
        CatalogueCaption("Read only, as a list")
        MonarchGanttTimeline(
            items = GanttItems,
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {},
            state = rememberMonarchGanttTimelineState(
                initialStart = GanttJan5,
                initialZoom = MonarchGanttTimelineZoom.Quarter,
                initialView = MonarchGanttTimelineView.List,
            ),
            dependencies = GanttLinks,
            milestones = GanttMilestones,
            today = GanttToday,
            toolbar = false,
        )
    }
}
