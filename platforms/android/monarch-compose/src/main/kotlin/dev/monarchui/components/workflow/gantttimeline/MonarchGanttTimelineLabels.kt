package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchGanttTimeline`. Every `null` keeps the built-in string
 * `monarch_gantttimeline_<key>`. Dates given to the functions are already formatted.
 *
 * @property timeline the name of the chart and the list read by TalkBack, "Timeline".
 * @property rowHeader the heading of the row-label column, "Item".
 * @property zoom the name of the zoom menu, "Zoom".
 * @property zoomName the name of a zoom level: "Weeks", "Months", "Quarters", "Years".
 * @property earlier the name of the backward arrow, "Earlier".
 * @property later the name of the forward arrow, "Later".
 * @property today the Today button and the label of the today line, "Today".
 * @property view the name of the chart and list switch, "View".
 * @property chart the chart choice, "Chart".
 * @property list the list choice, "List".
 * @property milestones the label of the milestone lane and the list's milestone heading, "Milestones".
 * @property hint the visible hint above an editable chart.
 * @property bar what TalkBack reads for a bar, from the label, the first and last day and the progress (`null`
 * without progress): "Design, Jan 12, 2026 to Jan 23, 2026, 60% complete".
 * @property conflict a conflict in words: "Conflict: Starts before Design ends".
 * @property milestone what TalkBack reads for a milestone: "Milestone Beta, Feb 9, 2026, done".
 * @property ended the row text of an item that ended before the window: "Ended Jan 9, 2026".
 * @property starts the row text of an item that starts after the window: "Starts Mar 2, 2026".
 * @property noDates the row text of an item without dates, "No dates".
 * @property unscheduled the heading of the items without dates: "Unscheduled · 2 without dates".
 * @property changed announced after a change: "Design: Jan 13, 2026 to Jan 24, 2026".
 * @property changeDates the list button that opens the date range picker, "Change dates".
 * @property save the confirm button of the date range picker, "Save".
 * @property cancel the dismiss button of the date range picker, "Cancel".
 * @property duration a length in days: "12 days".
 * @property progress a completion: "60% complete".
 * @property dependsOn the items an item depends on: "Depends on Design, Brief".
 * @property expanded the state of a parent whose children are shown, "Expanded".
 * @property collapsed the state of a parent whose children are hidden, "Collapsed".
 * @property children the visible caption of a parent row: "Hide 2 sub-items" or "Show 2 sub-items".
 * @property moveEarlier the TalkBack action that moves a bar a day earlier, "Move a day earlier".
 * @property moveLater the TalkBack action that moves a bar a day later, "Move a day later".
 * @property startEarlier the TalkBack action that starts an item a day earlier, "Start a day earlier".
 * @property startLater the TalkBack action that starts an item a day later, "Start a day later".
 * @property endEarlier the TalkBack action that ends an item a day earlier, "End a day earlier".
 * @property endLater the TalkBack action that ends an item a day later, "End a day later".
 * @property range the visible window in the toolbar: "Jan 5, 2026 – Apr 5, 2026".
 */
@Immutable
public class MonarchGanttTimelineLabels(
    public val timeline: String? = null,
    public val rowHeader: String? = null,
    public val zoom: String? = null,
    public val zoomName: ((MonarchGanttTimelineZoom) -> String)? = null,
    public val earlier: String? = null,
    public val later: String? = null,
    public val today: String? = null,
    public val view: String? = null,
    public val chart: String? = null,
    public val list: String? = null,
    public val milestones: String? = null,
    public val hint: String? = null,
    public val bar: ((label: String, start: String, end: String, progress: String?) -> String)? = null,
    public val conflict: ((message: String) -> String)? = null,
    public val milestone: ((label: String, date: String, done: Boolean) -> String)? = null,
    public val ended: ((date: String) -> String)? = null,
    public val starts: ((date: String) -> String)? = null,
    public val noDates: String? = null,
    public val unscheduled: ((count: Int) -> String)? = null,
    public val changed: ((label: String, start: String, end: String) -> String)? = null,
    public val changeDates: String? = null,
    public val save: String? = null,
    public val cancel: String? = null,
    public val duration: ((days: Int) -> String)? = null,
    public val progress: ((percent: String) -> String)? = null,
    public val dependsOn: ((names: String) -> String)? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
    public val children: ((count: Int, expanded: Boolean) -> String)? = null,
    public val moveEarlier: String? = null,
    public val moveLater: String? = null,
    public val startEarlier: String? = null,
    public val startLater: String? = null,
    public val endEarlier: String? = null,
    public val endLater: String? = null,
    public val range: ((from: String, to: String) -> String)? = null,
) {
    /**
     * A copy with the given strings replaced and the others kept, so an app or
     * a composite component can change a few labels without restating all.
     */
    @Suppress("LongParameterList")
    public fun copy(
        timeline: String? = this.timeline,
        rowHeader: String? = this.rowHeader,
        zoom: String? = this.zoom,
        zoomName: ((MonarchGanttTimelineZoom) -> String)? = this.zoomName,
        earlier: String? = this.earlier,
        later: String? = this.later,
        today: String? = this.today,
        view: String? = this.view,
        chart: String? = this.chart,
        list: String? = this.list,
        milestones: String? = this.milestones,
        hint: String? = this.hint,
        bar: ((label: String, start: String, end: String, progress: String?) -> String)? = this.bar,
        conflict: ((message: String) -> String)? = this.conflict,
        milestone: ((label: String, date: String, done: Boolean) -> String)? = this.milestone,
        ended: ((date: String) -> String)? = this.ended,
        starts: ((date: String) -> String)? = this.starts,
        noDates: String? = this.noDates,
        unscheduled: ((count: Int) -> String)? = this.unscheduled,
        changed: ((label: String, start: String, end: String) -> String)? = this.changed,
        changeDates: String? = this.changeDates,
        save: String? = this.save,
        cancel: String? = this.cancel,
        duration: ((days: Int) -> String)? = this.duration,
        progress: ((percent: String) -> String)? = this.progress,
        dependsOn: ((names: String) -> String)? = this.dependsOn,
        expanded: String? = this.expanded,
        collapsed: String? = this.collapsed,
        children: ((count: Int, expanded: Boolean) -> String)? = this.children,
        moveEarlier: String? = this.moveEarlier,
        moveLater: String? = this.moveLater,
        startEarlier: String? = this.startEarlier,
        startLater: String? = this.startLater,
        endEarlier: String? = this.endEarlier,
        endLater: String? = this.endLater,
        range: ((from: String, to: String) -> String)? = this.range,
    ): MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(
        timeline = timeline,
        rowHeader = rowHeader,
        zoom = zoom,
        zoomName = zoomName,
        earlier = earlier,
        later = later,
        today = today,
        view = view,
        chart = chart,
        list = list,
        milestones = milestones,
        hint = hint,
        bar = bar,
        conflict = conflict,
        milestone = milestone,
        ended = ended,
        starts = starts,
        noDates = noDates,
        unscheduled = unscheduled,
        changed = changed,
        changeDates = changeDates,
        save = save,
        cancel = cancel,
        duration = duration,
        progress = progress,
        dependsOn = dependsOn,
        expanded = expanded,
        collapsed = collapsed,
        children = children,
        moveEarlier = moveEarlier,
        moveLater = moveLater,
        startEarlier = startEarlier,
        startLater = startLater,
        endEarlier = endEarlier,
        endLater = endLater,
        range = range,
    )
}
