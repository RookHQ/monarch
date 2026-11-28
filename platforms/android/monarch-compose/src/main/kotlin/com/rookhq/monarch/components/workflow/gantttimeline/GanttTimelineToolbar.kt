package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControlSize
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.scheduling.calendartoolbar.MonarchCalendarToolbar
import com.rookhq.monarch.components.scheduling.calendartoolbar.MonarchCalendarToolbarLabels
import com.rookhq.monarch.components.scheduling.calendartoolbar.MonarchCalendarToolbarView

/**
 * The toolbar: a `MonarchCalendarToolbar` with Today, Earlier and Later and the visible range as its title, then the
 * zoom switch (a menu for more than three levels), the chart and list switch and the app's actions.
 */
@Composable
internal fun GanttTimelineToolbar(
    model: GanttTimelineModel,
    zoomLevels: List<MonarchGanttTimelineZoom>,
    previousIcon: @Composable () -> Unit,
    nextIcon: @Composable () -> Unit,
    actions: (@Composable RowScope.() -> Unit)?,
) {
    val text = model.text
    MonarchCalendarToolbar(
        title = model.range(),
        // The view only names the arrows, and the labels below replace those names; no switcher is shown.
        view = MonarchCalendarToolbarView.Month,
        onToday = { model.goToToday() },
        onPrevious = { model.step(forward = false) },
        onNext = { model.step(forward = true) },
        previousIcon = previousIcon,
        nextIcon = nextIcon,
        todayEnabled = model.today != null,
        todayBetween = true,
        labels = MonarchCalendarToolbarLabels(
            today = text.today,
            previous = { text.earlier },
            next = { text.later },
        ),
        actions = {
            if (zoomLevels.size in 2..SwitchMaxLevels && LocalDensity.current.fontScale <= SwitchMaxFontScale) {
                GanttTimelineZoomSwitch(model, zoomLevels)
            } else if (zoomLevels.size > 1) {
                GanttTimelineZoomMenu(model, zoomLevels)
            }
            GanttTimelineViewSwitch(model)
            actions?.invoke(this)
        },
    )
}

/** The zoom levels as a segmented control, like the web, for up to [SwitchMaxLevels] levels. */
@Composable
private fun GanttTimelineZoomSwitch(model: GanttTimelineModel, levels: List<MonarchGanttTimelineZoom>) {
    val text = model.text
    MonarchSegmentedControl(
        modifier = Modifier.semantics { contentDescription = text.zoom },
        size = MonarchSegmentedControlSize.Sm,
    ) {
        levels.forEach { level ->
            Segment(
                selected = level == model.zoom,
                onClick = { model.state.zoom = level },
                label = text.zoomName(level),
            )
        }
    }
}

private const val SwitchMaxLevels = 3

/** Above this text scale the three segments do not fit beside the view switch, so the menu is used. */
private const val SwitchMaxFontScale = 1.3f

/** A button that names the zoom and opens a `MonarchMenu` of the levels. */
@Composable
private fun GanttTimelineZoomMenu(model: GanttTimelineModel, levels: List<MonarchGanttTimelineZoom>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val text = model.text
    val current = text.zoomName(model.zoom)
    Box {
        MonarchButton(
            text = current,
            onClick = { expanded = true },
            modifier = Modifier.semantics {
                contentDescription = text.zoom
                stateDescription = current
            },
            size = MonarchButtonSize.Sm,
        )
        MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            levels.forEach { level ->
                val chosen = level == model.zoom
                MonarchMenuItem(
                    text = text.zoomName(level),
                    onClick = {
                        expanded = false
                        model.state.zoom = level
                    },
                    selected = chosen,
                )
            }
        }
    }
}

/** A segmented control that switches between the chart and the list. */
@Composable
private fun GanttTimelineViewSwitch(model: GanttTimelineModel) {
    val text = model.text
    MonarchSegmentedControl(
        modifier = Modifier.semantics { contentDescription = text.view },
        size = MonarchSegmentedControlSize.Sm,
    ) {
        MonarchGanttTimelineView.entries.forEach { view ->
            Segment(
                selected = model.state.view == view,
                onClick = { model.state.view = view },
                label = if (view == MonarchGanttTimelineView.Chart) text.chart else text.list,
            )
        }
    }
}

/** Moves the window one step of the zoom later, or earlier when [forward] is `false`. */
private fun GanttTimelineModel.step(forward: Boolean) {
    val step = MonarchGanttTimelineTokens.step(zoom)
    state.start = GanttTimelineDays.millis(anchor + if (forward) step else -step)
}

/** Moves the window to the week before today. */
private fun GanttTimelineModel.goToToday() {
    val day = today ?: return
    state.start = GanttTimelineDays.millis(GanttTimelineRules.todayAnchor(day))
}
