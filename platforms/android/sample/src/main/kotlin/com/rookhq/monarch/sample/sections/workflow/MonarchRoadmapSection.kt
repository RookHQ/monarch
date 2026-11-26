@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineDependency
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineMilestone
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineView
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineZoom
import com.rookhq.monarch.components.workflow.gantttimeline.rememberMonarchGanttTimelineState
import com.rookhq.monarch.components.workflow.roadmap.MonarchRoadmap
import com.rookhq.monarch.components.workflow.roadmap.MonarchRoadmapGroup
import com.rookhq.monarch.components.workflow.roadmap.MonarchRoadmapHealth
import com.rookhq.monarch.components.workflow.roadmap.MonarchRoadmapItem
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** Thursday 1 January 2026, 00:00 UTC. */
private const val RoadmapJan1 = 1_767_225_600_000L
private const val RoadmapDayMillis = 86_400_000L

private const val RoadmapToday = 13

private fun roadmapDay(offset: Int): Long = RoadmapJan1 + offset * RoadmapDayMillis

private val RoadmapGroups = listOf(
    MonarchRoadmapGroup(
        "growth",
        "Grow repeat customers",
        start = roadmapDay(0),
        end = roadmapDay(180),
        health = MonarchRoadmapHealth.OnTrack,
    ),
    MonarchRoadmapGroup(
        "quality",
        "Fewer support tickets",
        start = roadmapDay(31),
        end = roadmapDay(242),
        health = MonarchRoadmapHealth.AtRisk,
    ),
)

private val RoadmapItems = listOf(
    MonarchRoadmapItem(
        "p1",
        "Loyalty emails",
        start = roadmapDay(4),
        end = roadmapDay(78),
        groupId = "growth",
        progress = 0.4f,
        progressText = "8/20",
        health = MonarchRoadmapHealth.OnTrack,
    ),
    MonarchRoadmapItem(
        "p2",
        "Referral page",
        start = roadmapDay(59),
        end = roadmapDay(133),
        groupId = "growth",
        progress = 0.1f,
        progressText = "1/10",
        health = MonarchRoadmapHealth.NotReported,
    ),
    MonarchRoadmapItem(
        "p3",
        "Help center search",
        start = roadmapDay(32),
        end = roadmapDay(119),
        groupId = "quality",
        progress = 0.25f,
        progressText = "3/12",
        health = MonarchRoadmapHealth.AtRisk,
    ),
    MonarchRoadmapItem(
        "p4",
        "Crash fixes",
        start = roadmapDay(11),
        end = roadmapDay(57),
        groupId = "quality",
        progress = 0.7f,
        progressText = "14/20",
        health = MonarchRoadmapHealth.OffTrack,
    ),
    MonarchRoadmapItem("p5", "Billing revamp", progressText = "0/6"),
)

private val RoadmapLinks = listOf(MonarchGanttTimelineDependency("d1", "p4", "p3"))

private val RoadmapMilestones = listOf(MonarchGanttTimelineMilestone("m1", "Launch", roadmapDay(78), itemId = "p1"))

/** The catalogue page of Roadmap. */
object MonarchRoadmapSection : CataloguePage {
    override val title = "Roadmap"

    @Composable
    override fun Content() {
        var opened by remember { mutableStateOf<String?>(null) }
        CatalogueCaption("Projects by initiative, read only")
        MonarchRoadmap(
            items = RoadmapItems,
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {
                MonarchIcon(
                    SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
            state = rememberMonarchGanttTimelineState(
                initialStart = RoadmapJan1,
                initialZoom = MonarchGanttTimelineZoom.Quarter,
            ),
            groups = RoadmapGroups,
            dependencies = RoadmapLinks,
            milestones = RoadmapMilestones,
            today = roadmapDay(RoadmapToday),
            onOpen = { opened = it.label },
        )
        opened?.let { CatalogueCaption("Opened: $it") }
        var items by remember { mutableStateOf(RoadmapItems) }
        CatalogueCaption("Replanning, as a list")
        MonarchRoadmap(
            items = items,
            previousIcon = {},
            nextIcon = {},
            state = rememberMonarchGanttTimelineState(
                initialStart = RoadmapJan1,
                initialZoom = MonarchGanttTimelineZoom.Quarter,
                initialView = MonarchGanttTimelineView.List,
            ),
            groups = RoadmapGroups,
            dependencies = RoadmapLinks,
            today = roadmapDay(RoadmapToday),
            toolbar = false,
            onChange = { item, dates ->
                items = items.map {
                    if (it.id != item.id) {
                        it
                    } else {
                        MonarchRoadmapItem(
                            id = it.id,
                            label = it.label,
                            start = dates.start,
                            end = dates.end,
                            groupId = it.groupId,
                            progress = it.progress,
                            progressText = it.progressText,
                            health = it.health,
                            tone = it.tone,
                        )
                    }
                }
            },
        )
    }
}
