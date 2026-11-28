@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.workflow.roadmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineDependency
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineMilestone
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineState
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineView
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineZoom
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.TimeZone

/** Thursday 1 January 2026, 00:00 UTC. */
private const val RoadmapPreviewJan1 = 1_767_225_600_000L
private const val RoadmapPreviewDay = 86_400_000L
private val RoadmapPreviewUtc: TimeZone = TimeZone.getTimeZone("UTC")

private const val RoadmapPreviewLaunch = 78
private const val RoadmapPreviewToday = 13

private fun roadmapPreviewDay(offset: Int): Long = RoadmapPreviewJan1 + offset * RoadmapPreviewDay

private val RoadmapPreviewGroups = listOf(
    MonarchRoadmapGroup(
        "growth",
        "Grow repeat customers",
        start = roadmapPreviewDay(0),
        end = roadmapPreviewDay(180),
        health = MonarchRoadmapHealth.OnTrack,
    ),
    MonarchRoadmapGroup("quality", "Fewer support tickets", health = MonarchRoadmapHealth.AtRisk),
)

private val RoadmapPreviewItems = listOf(
    MonarchRoadmapItem(
        "p1",
        "Loyalty emails",
        start = roadmapPreviewDay(4),
        end = roadmapPreviewDay(78),
        groupId = "growth",
        progress = 0.4f,
        progressText = "8/20",
        health = MonarchRoadmapHealth.OnTrack,
    ),
    MonarchRoadmapItem(
        "p2",
        "Referral page",
        start = roadmapPreviewDay(59),
        end = roadmapPreviewDay(133),
        groupId = "growth",
        progressText = "1/10",
        health = MonarchRoadmapHealth.NotReported,
    ),
    MonarchRoadmapItem(
        "p3",
        "Help center search",
        start = roadmapPreviewDay(32),
        end = roadmapPreviewDay(119),
        groupId = "quality",
        progress = 0.25f,
        health = MonarchRoadmapHealth.AtRisk,
    ),
    MonarchRoadmapItem(
        "p4",
        "Crash fixes",
        start = roadmapPreviewDay(11),
        end = roadmapPreviewDay(57),
        groupId = "quality",
        progress = 0.7f,
        health = MonarchRoadmapHealth.OffTrack,
    ),
    MonarchRoadmapItem("p5", "Billing revamp", progressText = "0/6"),
)

@Composable
private fun RoadmapPreviewContent(view: MonarchGanttTimelineView) {
    MonarchRoadmap(
        items = RoadmapPreviewItems,
        previousIcon = {},
        nextIcon = {},
        state = remember {
            MonarchGanttTimelineState(start = RoadmapPreviewJan1, zoom = MonarchGanttTimelineZoom.Quarter, view = view)
        },
        groups = RoadmapPreviewGroups,
        dependencies = listOf(MonarchGanttTimelineDependency("d1", "p4", "p3")),
        milestones = listOf(
            MonarchGanttTimelineMilestone("m1", "Launch", roadmapPreviewDay(RoadmapPreviewLaunch), itemId = "p1"),
        ),
        today = roadmapPreviewDay(RoadmapPreviewToday),
        timeZone = RoadmapPreviewUtc.id,
        toolbar = false,
        onOpen = {},
    )
}

@MonarchPreviews
@Composable
internal fun MonarchRoadmapChartPreview() {
    PreviewFrame { RoadmapPreviewContent(MonarchGanttTimelineView.Chart) }
}

@MonarchPreviews
@Composable
internal fun MonarchRoadmapListPreview() {
    PreviewFrame { RoadmapPreviewContent(MonarchGanttTimelineView.List) }
}
