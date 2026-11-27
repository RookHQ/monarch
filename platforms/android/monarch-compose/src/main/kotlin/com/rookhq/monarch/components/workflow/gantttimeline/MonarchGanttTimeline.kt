package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchTimeZone
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A Gantt timeline: items as bars on a calendar from their first to their last day, with dependency arrows,
 * milestones, a today line, zoom from weeks to years, and a list view that writes every date in words and changes
 * dates with the Material 3 date range picker. Data in, changes out: the app saves each change and passes the new
 * items.
 *
 * Built by Monarch from Monarch and Material 3 parts, because Material 3 has no timeline chart. The toolbar is a
 * `MonarchCalendarToolbar` with a zoom menu and a chart and list switch (Material 3 segmented buttons). The chart
 * keeps the row labels at the start and scrolls the calendar sideways; its time axis runs left to right in every
 * layout direction, as on the web. When the timeline is editable, a bar is moved by touching and holding it and then
 * dragging, its ends by holding near an end; a hardware keyboard moves a focused bar with the arrow keys (Shift for
 * the end, Alt for the start); TalkBack offers the same moves as custom actions. The list is the alternative to the
 * chart on a phone and with TalkBack: nothing in the chart is only available by dragging.
 *
 * The timeline does not scroll vertically: place it in a scrolling screen. Dates are day keys (the epoch millisecond
 * of the day's midnight in UTC, as in the Material 3 `DatePickerState`), formatted in the locale of the
 * configuration.
 *
 * @param items the rows. Nesting comes from `parentId`, grouping from `groupId`.
 * @param previousIcon the backward chevron of the toolbar, for example a `MonarchIcon` with `mirrorInRtl = true` and
 * a `null` description. Monarch bundles no icons.
 * @param nextIcon the forward chevron of the toolbar.
 * @param modifier the modifier of the timeline.
 * @param state the first visible day, the zoom, chart or list and the collapsed parents. See
 * [rememberMonarchGanttTimelineState].
 * @param groups heading rows that group items.
 * @param dependencies links between items, drawn as arrows.
 * @param milestones dated checkpoints, drawn as diamonds.
 * @param zoomLevels the zoom levels offered in the toolbar. With one level or none there is no zoom menu.
 * @param undated where top-level items without dates go: a list under the chart, or rows that say "No dates".
 * @param today any time on the day of the today line, in epoch milliseconds, read in [timeZone], or `null` for no
 * line and no Today button. Pass a fixed value in tests and previews.
 * @param timeZone the zone [today] is read in. An IANA id such as `"Europe/Berlin"`. `null`, or an id the device does
 * not know, is the device's zone.
 * @param toolbar shows the toolbar.
 * @param onChange called with an item and its new dates after a drag, a key press, a TalkBack action or the date
 * range picker. Without it the timeline is read-only.
 * @param onOpen makes each bar and list row a button that opens the item.
 * @param labels replacements for the built-in strings.
 * @param toolbarActions extra controls at the end of the toolbar.
 * @param itemMeta extra content under an item's name, after its `meta` text, in the chart's row labels and the
 * list: for example a status badge or an assignee avatar (the web's `meta` node). It is read by TalkBack where it
 * is drawn.
 * @param groupMeta extra content under a group's heading, after its `meta` text.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchGanttTimeline(
    items: List<MonarchGanttTimelineItem>,
    previousIcon: @Composable () -> Unit,
    nextIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: MonarchGanttTimelineState = rememberMonarchGanttTimelineState(),
    groups: List<MonarchGanttTimelineGroup> = emptyList(),
    dependencies: List<MonarchGanttTimelineDependency> = emptyList(),
    milestones: List<MonarchGanttTimelineMilestone> = emptyList(),
    zoomLevels: List<MonarchGanttTimelineZoom> = listOf(
        MonarchGanttTimelineZoom.Month,
        MonarchGanttTimelineZoom.Quarter,
        MonarchGanttTimelineZoom.Year,
    ),
    undated: MonarchGanttTimelineUndated = MonarchGanttTimelineUndated.List,
    today: Long? = System.currentTimeMillis(),
    timeZone: String? = null,
    toolbar: Boolean = true,
    onChange: ((MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Unit)? = null,
    onOpen: ((MonarchGanttTimelineItem) -> Unit)? = null,
    labels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
    toolbarActions: (@Composable RowScope.() -> Unit)? = null,
    itemMeta: (@Composable (MonarchGanttTimelineItem) -> Unit)? = null,
    groupMeta: (@Composable (MonarchGanttTimelineGroup) -> Unit)? = null,
) {
    val text = ganttTimelineText(labels)
    val locale = LocalConfiguration.current.locales[0]
    val formats = remember(locale) { GanttTimelineFormats(locale) }
    val zone = monarchTimeZone(timeZone)
    val todayDay = today?.let { GanttTimelineDays.localDay(it, zone) }
    val collapsed = state.collapsed
    val lines = remember(items, groups, collapsed, undated) {
        GanttTimelineRules.lines(items, groups, collapsed, undated)
    }
    var announcement by remember { mutableStateOf("") }
    val model = GanttTimelineModel(
        items = items,
        groups = groups,
        dependencies = dependencies,
        milestones = milestones,
        lines = lines,
        state = state,
        anchor = GanttTimelineRules.anchor(state.start, todayDay, items),
        today = todayDay,
        onChange = onChange,
        onOpen = onOpen,
        text = text,
        formats = formats,
        announce = { announcement = it },
        itemMeta = itemMeta,
        groupMeta = groupMeta,
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchGanttTimelineTokens.Gap),
    ) {
        if (toolbar) GanttTimelineToolbar(model, zoomLevels, previousIcon, nextIcon, toolbarActions)
        when (state.view) {
            MonarchGanttTimelineView.Chart -> {
                if (model.editable) {
                    MonarchText(
                        text = text.hint,
                        style = MonarchTextStyle.Caption,
                        color = MonarchGanttTimelineTokens.mutedColor(MonarchTheme.colors),
                    )
                }
                GanttTimelineChart(model)
                if (lines.unscheduled.isNotEmpty()) GanttTimelineUnscheduled(model)
            }
            MonarchGanttTimelineView.List -> GanttTimelineList(model)
        }
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}
