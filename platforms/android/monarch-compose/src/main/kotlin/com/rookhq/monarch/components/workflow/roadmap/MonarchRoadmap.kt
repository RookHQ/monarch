package com.rookhq.monarch.components.workflow.roadmap

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimeline
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineDates
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineDependency
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineLabels
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineMilestone
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineState
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineUndated
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineZoom
import com.rookhq.monarch.components.workflow.gantttimeline.rememberMonarchGanttTimelineState
import com.rookhq.monarch.internal.monarchString

/**
 * A roadmap: projects over months and quarters, grouped by initiative, with progress, the health last reported,
 * milestones and dependencies between projects. A planning view for reading, preset on `MonarchGanttTimeline`: it
 * opens at the quarter zoom, writes each project's progress text and health in words in its row heading, counts the
 * projects of each group, tints at-risk and off-track bars, and keeps projects without dates as rows that say
 * "No dates" so none disappears.
 *
 * Built from `MonarchGanttTimeline` (and through it Material 3 menus, segmented buttons and the date range
 * picker), because Material 3 has no timeline chart. The chart, the list view, TalkBack actions and keyboard moves
 * are the timeline's. Read-only unless [onChange] is given.
 *
 * The roadmap does not scroll vertically: place it in a scrolling screen. Dates are day keys (the epoch millisecond
 * of the day's midnight in UTC, as in the Material 3 `DatePickerState`).
 *
 * @param items the projects, in order within their groups.
 * @param previousIcon the backward chevron of the toolbar, for example a `MonarchIcon` with `mirrorInRtl = true` and
 * a `null` description. Monarch bundles no icons.
 * @param nextIcon the forward chevron of the toolbar.
 * @param modifier the modifier of the roadmap.
 * @param state the first visible day, the zoom (quarter at first), chart or list. See
 * `rememberMonarchGanttTimelineState`.
 * @param groups heading rows, in order; projects without a known group follow them.
 * @param dependencies links between projects, drawn as arrows and written in the list.
 * @param milestones dated checkpoints: on a project's row with `itemId`, otherwise in a lane under the header.
 * @param zoomLevels the zoom levels offered in the toolbar.
 * @param today any time on the day of the today line, in epoch milliseconds, read in [timeZone], or `null` for no
 * line and no Today button. Pass a fixed value in tests and previews.
 * @param timeZone the zone [today] is read in. An IANA id such as `"Europe/Berlin"`. `null`, or an id the device does
 * not know, is the device's zone.
 * @param toolbar shows the toolbar.
 * @param onChange makes dates editable and is called with the project and its new dates. Without it the roadmap is
 * read-only.
 * @param onOpen makes each bar and list row a button that opens the project.
 * @param labels replacements for the roadmap's own strings.
 * @param timelineLabels replacements for the timeline's strings. The chart is named "Roadmap" and the row header
 * says "Project" unless replaced here.
 * @param toolbarActions extra controls at the end of the toolbar.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList")
@Composable
public fun MonarchRoadmap(
    items: List<MonarchRoadmapItem>,
    previousIcon: @Composable () -> Unit,
    nextIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: MonarchGanttTimelineState = rememberMonarchGanttTimelineState(
        initialZoom = MonarchGanttTimelineZoom.Quarter,
    ),
    groups: List<MonarchRoadmapGroup> = emptyList(),
    dependencies: List<MonarchGanttTimelineDependency> = emptyList(),
    milestones: List<MonarchGanttTimelineMilestone> = emptyList(),
    zoomLevels: List<MonarchGanttTimelineZoom> = listOf(
        MonarchGanttTimelineZoom.Month,
        MonarchGanttTimelineZoom.Quarter,
        MonarchGanttTimelineZoom.Year,
    ),
    today: Long? = System.currentTimeMillis(),
    timeZone: String? = null,
    toolbar: Boolean = true,
    onChange: ((MonarchRoadmapItem, MonarchGanttTimelineDates) -> Unit)? = null,
    onOpen: ((MonarchRoadmapItem) -> Unit)? = null,
    labels: MonarchRoadmapLabels = MonarchRoadmapLabels(),
    timelineLabels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
    toolbarActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    // The timeline recomputes its rows when it gets new item instances, so map only when the input changes.
    val rows = remember(items, groups, labels, resources, locale) {
        MonarchRoadmapRules.rows(items, groups, MonarchRoadmapText.of(labels, resources))
    }
    val timeline = monarchString(null, R.string.monarch_roadmap_timeline)
    val rowHeader = monarchString(null, R.string.monarch_roadmap_row_header)
    val mergedLabels = remember(timelineLabels, timeline, rowHeader) {
        MonarchRoadmapRules.timelineLabels(timelineLabels, timeline, rowHeader)
    }
    val text = remember(labels, resources, locale) { MonarchRoadmapText.of(labels, resources) }
    val counts = remember(items) { items.mapNotNull { it.groupId }.groupingBy { it }.eachCount() }
    val groupsById = remember(groups) { groups.associateBy { it.id } }
    MonarchGanttTimeline(
        items = rows.items,
        previousIcon = previousIcon,
        nextIcon = nextIcon,
        modifier = modifier,
        state = state,
        groups = rows.groups,
        dependencies = dependencies,
        milestones = milestones,
        zoomLevels = zoomLevels,
        undated = MonarchGanttTimelineUndated.Row,
        today = today,
        timeZone = timeZone,
        toolbar = toolbar,
        onChange = onChange?.let { change ->
            { item, dates -> rows.byId[item.id]?.let { change(it, dates) } }
        },
        onOpen = onOpen?.let { open -> { item -> rows.byId[item.id]?.let(open) } },
        labels = mergedLabels,
        toolbarActions = toolbarActions,
        itemMeta = { item ->
            val health = rows.byId[item.id]?.health
            MonarchRoadmapHealthChip(health, health?.let(text.health).orEmpty())
        },
        groupMeta = { group ->
            groupsById[group.id]?.let {
                val description = MonarchRoadmapRules.groupDescription(it, counts[it.id] ?: 0, text)
                MonarchRoadmapHealthChip(it.health, it.health?.let(text.health).orEmpty(), description)
            }
        },
    )
}
