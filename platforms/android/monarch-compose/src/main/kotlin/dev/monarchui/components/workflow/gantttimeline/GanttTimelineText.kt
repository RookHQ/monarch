package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import dev.monarchui.R
import dev.monarchui.internal.monarchString

/** The built-in strings of MonarchGanttTimeline, resolved once with the app's replacements. */
@Suppress("LongParameterList")
internal class GanttTimelineText(
    val timeline: String,
    val rowHeader: String,
    val zoom: String,
    val zoomName: (MonarchGanttTimelineZoom) -> String,
    val earlier: String,
    val later: String,
    val today: String,
    val view: String,
    val chart: String,
    val list: String,
    val milestones: String,
    val hint: String,
    val bar: (String, String, String, String?) -> String,
    val conflict: (String) -> String,
    val milestone: (String, String, Boolean) -> String,
    val ended: (String) -> String,
    val starts: (String) -> String,
    val noDates: String,
    val unscheduled: (Int) -> String,
    val changed: (String, String, String) -> String,
    val changeDates: String,
    val save: String,
    val cancel: String,
    val duration: (Int) -> String,
    val progress: (String) -> String,
    val dependsOn: (String) -> String,
    val expanded: String,
    val collapsed: String,
    val children: (Int, Boolean) -> String,
    val moveEarlier: String,
    val moveLater: String,
    val startEarlier: String,
    val startLater: String,
    val endEarlier: String,
    val endLater: String,
    val range: (String, String) -> String,
)

/** Resolves [labels] against the built-in strings. Each `?:` only picks the app's string over the built-in one. */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun ganttTimelineText(labels: MonarchGanttTimelineLabels): GanttTimelineText {
    val resources = LocalResources.current
    val progress: (String) -> String =
        labels.progress ?: { percent -> resources.getString(R.string.monarch_gantttimeline_progress, percent) }
    return GanttTimelineText(
        timeline = monarchString(labels.timeline, R.string.monarch_gantttimeline_timeline),
        rowHeader = monarchString(labels.rowHeader, R.string.monarch_gantttimeline_row_header),
        zoom = monarchString(labels.zoom, R.string.monarch_gantttimeline_zoom),
        zoomName = labels.zoomName ?: { zoom -> resources.getString(MonarchGanttTimelineTokens.zoomNameRes(zoom)) },
        earlier = monarchString(labels.earlier, R.string.monarch_gantttimeline_earlier),
        later = monarchString(labels.later, R.string.monarch_gantttimeline_later),
        today = monarchString(labels.today, R.string.monarch_gantttimeline_today),
        view = monarchString(labels.view, R.string.monarch_gantttimeline_view),
        chart = monarchString(labels.chart, R.string.monarch_gantttimeline_chart),
        list = monarchString(labels.list, R.string.monarch_gantttimeline_list),
        milestones = monarchString(labels.milestones, R.string.monarch_gantttimeline_milestones),
        hint = monarchString(labels.hint, R.string.monarch_gantttimeline_hint),
        bar = labels.bar ?: { label, start, end, percent ->
            val dates = if (start == end) {
                resources.getString(R.string.monarch_gantttimeline_bar_day, label, start)
            } else {
                resources.getString(R.string.monarch_gantttimeline_bar, label, start, end)
            }
            if (percent == null) dates else "$dates, ${progress(percent)}"
        },
        conflict = labels.conflict ?: { resources.getString(R.string.monarch_gantttimeline_conflict, it) },
        milestone = labels.milestone ?: { label, date, done ->
            val id = if (done) {
                R.string.monarch_gantttimeline_milestone_done
            } else {
                R.string.monarch_gantttimeline_milestone
            }
            resources.getString(id, label, date)
        },
        ended = labels.ended ?: { resources.getString(R.string.monarch_gantttimeline_ended, it) },
        starts = labels.starts ?: { resources.getString(R.string.monarch_gantttimeline_starts, it) },
        noDates = monarchString(labels.noDates, R.string.monarch_gantttimeline_no_dates),
        unscheduled = labels.unscheduled ?: { count ->
            resources.getQuantityString(R.plurals.monarch_gantttimeline_unscheduled, count, count)
        },
        changed = labels.changed ?: { label, start, end ->
            if (start == end) {
                resources.getString(R.string.monarch_gantttimeline_bar_day, label, start)
            } else {
                resources.getString(R.string.monarch_gantttimeline_changed, label, start, end)
            }
        },
        changeDates = monarchString(labels.changeDates, R.string.monarch_gantttimeline_change_dates),
        save = monarchString(labels.save, R.string.monarch_gantttimeline_save),
        cancel = monarchString(labels.cancel, R.string.monarch_gantttimeline_cancel),
        duration = labels.duration ?: { days ->
            resources.getQuantityString(R.plurals.monarch_gantttimeline_duration, days, days)
        },
        progress = progress,
        dependsOn = labels.dependsOn ?: { resources.getString(R.string.monarch_gantttimeline_depends_on, it) },
        expanded = monarchString(labels.expanded, R.string.monarch_gantttimeline_expanded),
        collapsed = monarchString(labels.collapsed, R.string.monarch_gantttimeline_collapsed),
        children = labels.children ?: { count, expanded ->
            val id = if (expanded) {
                R.plurals.monarch_gantttimeline_hide_children
            } else {
                R.plurals.monarch_gantttimeline_show_children
            }
            resources.getQuantityString(id, count, count)
        },
        moveEarlier = monarchString(labels.moveEarlier, R.string.monarch_gantttimeline_move_earlier),
        moveLater = monarchString(labels.moveLater, R.string.monarch_gantttimeline_move_later),
        startEarlier = monarchString(labels.startEarlier, R.string.monarch_gantttimeline_start_earlier),
        startLater = monarchString(labels.startLater, R.string.monarch_gantttimeline_start_later),
        endEarlier = monarchString(labels.endEarlier, R.string.monarch_gantttimeline_end_earlier),
        endLater = monarchString(labels.endLater, R.string.monarch_gantttimeline_end_later),
        range = labels.range ?: { from, to -> resources.getString(R.string.monarch_gantttimeline_range, from, to) },
    )
}
