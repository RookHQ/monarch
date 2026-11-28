@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.util.TimeZone

/** Monday 5 January 2026, 00:00 UTC. */
private const val PreviewJan5 = 1_767_571_200_000L
private const val PreviewDay = 86_400_000L
private val PreviewUtc: TimeZone = TimeZone.getTimeZone("UTC")

private const val PreviewFirstDay = 5

private fun previewJan(day: Int): Long = PreviewJan5 + (day - PreviewFirstDay) * PreviewDay

private val PreviewItems = listOf(
    MonarchGanttTimelineItem(
        "brief",
        "Write the brief",
        start = previewJan(5),
        end = previewJan(9),
        progress = 1f,
        tone = MonarchTone.Success,
    ),
    MonarchGanttTimelineItem("design", "Design", start = previewJan(12), end = previewJan(23), progress = 0.6f),
    MonarchGanttTimelineItem(
        "build",
        "Build",
        start = previewJan(21),
        end = previewJan(44),
        conflict = "Starts before Design ends",
    ),
    MonarchGanttTimelineItem("api", "API changes", start = previewJan(26), end = previewJan(35), parentId = "build"),
    MonarchGanttTimelineItem("launch", "Launch", end = previewJan(51)),
    MonarchGanttTimelineItem("research", "Pricing research"),
)

private val PreviewLinks = listOf(
    MonarchGanttTimelineDependency("l1", "brief", "design"),
    MonarchGanttTimelineDependency("l2", "design", "build", conflict = true),
)

private val PreviewToday = previewJan(14)

private val PreviewMilestones = listOf(MonarchGanttTimelineMilestone("m1", "Design review", previewJan(23)))

@Composable
private fun PreviewTimeline(view: MonarchGanttTimelineView, zoom: MonarchGanttTimelineZoom) {
    MonarchGanttTimeline(
        items = PreviewItems,
        previousIcon = {},
        nextIcon = {},
        state = remember { MonarchGanttTimelineState(zoom = zoom, view = view) },
        dependencies = PreviewLinks,
        milestones = PreviewMilestones,
        today = PreviewToday,
        timeZone = PreviewUtc.id,
        toolbar = false,
        onChange = { _, _ -> },
        onOpen = {},
    )
}

@MonarchPreviews
@Composable
internal fun MonarchGanttTimelineChartPreview() {
    PreviewFrame { PreviewTimeline(MonarchGanttTimelineView.Chart, MonarchGanttTimelineZoom.Week) }
}

@MonarchPreviews
@Composable
internal fun MonarchGanttTimelineQuarterPreview() {
    PreviewFrame { PreviewTimeline(MonarchGanttTimelineView.Chart, MonarchGanttTimelineZoom.Quarter) }
}

@MonarchPreviews
@Composable
internal fun MonarchGanttTimelineListPreview() {
    PreviewFrame { PreviewTimeline(MonarchGanttTimelineView.List, MonarchGanttTimelineZoom.Month) }
}
