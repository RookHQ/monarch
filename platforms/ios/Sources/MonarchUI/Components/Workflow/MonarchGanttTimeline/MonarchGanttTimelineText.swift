/// The strings of ``MonarchGanttTimeline``: the app's replacement when there
/// is one, otherwise the built-in string. Pure, so tests read every string.
struct MonarchGanttTimelineText: Sendable {
    let labels: MonarchGanttTimelineLabels

    var timeline: String { labels.timeline ?? MonarchGanttTimelineStrings.timeline }
    var rowHeader: String { labels.rowHeader ?? MonarchGanttTimelineStrings.rowHeader }
    var zoom: String { labels.zoom ?? MonarchGanttTimelineStrings.zoom }
    func zoomName(_ zoom: MonarchGanttTimelineZoom) -> String {
        labels.zoomName?(zoom) ?? MonarchGanttTimelineStrings.zoomName(zoom)
    }
    var earlier: String { labels.earlier ?? MonarchGanttTimelineStrings.earlier }
    var later: String { labels.later ?? MonarchGanttTimelineStrings.later }
    var today: String { labels.today ?? MonarchGanttTimelineStrings.today }
    var view: String { labels.view ?? MonarchGanttTimelineStrings.view }
    var chart: String { labels.chart ?? MonarchGanttTimelineStrings.chart }
    var list: String { labels.list ?? MonarchGanttTimelineStrings.list }
    func viewName(_ view: MonarchGanttTimelineView) -> String {
        view == .chart ? chart : list
    }
    var milestones: String { labels.milestones ?? MonarchGanttTimelineStrings.milestones }
    var hint: String { labels.hint ?? MonarchGanttTimelineStrings.hint }
    var noDates: String { labels.noDates ?? MonarchGanttTimelineStrings.noDates }
    var changeDates: String { labels.changeDates ?? MonarchGanttTimelineStrings.changeDates }
    var startDate: String { labels.startDate ?? MonarchGanttTimelineStrings.startDate }
    var endDate: String { labels.endDate ?? MonarchGanttTimelineStrings.endDate }
    var save: String { labels.save ?? MonarchGanttTimelineStrings.save }
    var cancel: String { labels.cancel ?? MonarchGanttTimelineStrings.cancel }
    var expanded: String { labels.expanded ?? MonarchGanttTimelineStrings.expanded }
    var collapsed: String { labels.collapsed ?? MonarchGanttTimelineStrings.collapsed }
    var moveEarlier: String { labels.moveEarlier ?? MonarchGanttTimelineStrings.moveEarlier }
    var moveLater: String { labels.moveLater ?? MonarchGanttTimelineStrings.moveLater }
    var startEarlier: String { labels.startEarlier ?? MonarchGanttTimelineStrings.startEarlier }
    var startLater: String { labels.startLater ?? MonarchGanttTimelineStrings.startLater }
    var endEarlier: String { labels.endEarlier ?? MonarchGanttTimelineStrings.endEarlier }
    var endLater: String { labels.endLater ?? MonarchGanttTimelineStrings.endLater }

    /// "60% complete".
    func progress(_ percent: String) -> String {
        labels.progress?(percent) ?? MonarchGanttTimelineStrings.progress(percent)
    }

    /// "Design, Jan 12, 2026 to Jan 24, 2026, 60% complete"; one date when both are the same day.
    func bar(_ label: String, start: String, end: String, progress percent: String?) -> String {
        if let custom = labels.bar { return custom(label, start, end, percent) }
        let dates =
            start == end
            ? MonarchGanttTimelineStrings.barDay(label, start)
            : MonarchGanttTimelineStrings.bar(label, start, end)
        guard let percent else { return dates }
        return "\(dates), \(progress(percent))"
    }

    func conflict(_ message: String) -> String {
        labels.conflict?(message) ?? MonarchGanttTimelineStrings.conflict(message)
    }

    func milestone(_ label: String, date: String, isDone: Bool) -> String {
        labels.milestone?(label, date, isDone) ?? MonarchGanttTimelineStrings.milestone(label, date, isDone: isDone)
    }

    func ended(_ date: String) -> String { labels.ended?(date) ?? MonarchGanttTimelineStrings.ended(date) }
    func starts(_ date: String) -> String { labels.starts?(date) ?? MonarchGanttTimelineStrings.starts(date) }

    func unscheduled(_ count: Int) -> String {
        labels.unscheduled?(count) ?? MonarchGanttTimelineStrings.unscheduled(count)
    }

    func changed(_ label: String, start: String, end: String) -> String {
        labels.changed?(label, start, end) ?? MonarchGanttTimelineStrings.changed(label, start, end)
    }

    func duration(_ days: Int) -> String { labels.duration?(days) ?? MonarchGanttTimelineStrings.duration(days) }

    func dependsOn(_ names: String) -> String {
        labels.dependsOn?(names) ?? MonarchGanttTimelineStrings.dependsOn(names)
    }

    func children(_ count: Int, isExpanded: Bool) -> String {
        labels.children?(count, isExpanded) ?? MonarchGanttTimelineStrings.children(count, isExpanded: isExpanded)
    }

    func range(_ from: String, _ to: String) -> String {
        labels.range?(from, to) ?? MonarchGanttTimelineStrings.range(from, to)
    }
}
