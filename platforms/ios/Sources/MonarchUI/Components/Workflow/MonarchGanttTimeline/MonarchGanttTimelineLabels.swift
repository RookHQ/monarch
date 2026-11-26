/// Replacements for the built-in strings of ``MonarchGanttTimeline``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchGanttTimelineLabels: Sendable {
    /// The spoken name of the chart and the list. Built in: "Timeline".
    public var timeline: String?
    /// The heading of the row-label column. Built in: "Item".
    public var rowHeader: String?
    /// The spoken name of the zoom menu. Built in: "Zoom".
    public var zoom: String?
    /// The name of a zoom level. Built in: "Weeks", "Months", "Quarters", "Years".
    public var zoomName: (@Sendable (MonarchGanttTimelineZoom) -> String)?
    /// The spoken name of the backward arrow. Built in: "Earlier".
    public var earlier: String?
    /// The spoken name of the forward arrow. Built in: "Later".
    public var later: String?
    /// The Today button and the label of the today line. Built in: "Today".
    public var today: String?
    /// The spoken name of the chart and list switch. Built in: "View".
    public var view: String?
    /// The chart choice of the switch. Built in: "Chart".
    public var chart: String?
    /// The list choice of the switch. Built in: "List".
    public var list: String?
    /// The label of the milestone lane and the heading of the milestones in the list. Built in: "Milestones".
    public var milestones: String?
    /// The visible hint above an editable chart. Built in: "Touch and hold a bar to move it, or one of its ends to change that date."
    public var hint: String?
    /// What VoiceOver reads for a bar, from the label, the formatted first and last days and the formatted progress. Built in: "Design, Jan 12, 2026 to Jan 24, 2026, 60% complete".
    public var bar: (@Sendable (_ label: String, _ start: String, _ end: String, _ progress: String?) -> String)?
    /// A conflict in words, from the app's message. Built in: "Conflict: …".
    public var conflict: (@Sendable (String) -> String)?
    /// What VoiceOver reads for a milestone. Built in: "Milestone Design review, Jan 23, 2026", with ", done" when reached.
    public var milestone: (@Sendable (_ label: String, _ date: String, _ isDone: Bool) -> String)?
    /// The row text of an item that ended before the window. Built in: "Ended …".
    public var ended: (@Sendable (String) -> String)?
    /// The row text of an item that starts after the window. Built in: "Starts …".
    public var starts: (@Sendable (String) -> String)?
    /// The row text of an item without dates. Built in: "No dates".
    public var noDates: String?
    /// The heading of the items without dates, from their number. Built in: "Unscheduled · 2 without dates".
    public var unscheduled: (@Sendable (Int) -> String)?
    /// Announced after a change. Built in: "Design: Jan 12, 2026 to Jan 24, 2026".
    public var changed: (@Sendable (_ label: String, _ start: String, _ end: String) -> String)?
    /// The button of a list row that opens the date sheet. Built in: "Change dates".
    public var changeDates: String?
    /// The first date picker of the date sheet. Built in: "Start".
    public var startDate: String?
    /// The second date picker of the date sheet. Built in: "End".
    public var endDate: String?
    /// The confirm button of the date sheet. Built in: "Save".
    public var save: String?
    /// The dismiss button of the date sheet. Built in: "Cancel".
    public var cancel: String?
    /// The length of an item in days. Built in: "1 day", "12 days".
    public var duration: (@Sendable (Int) -> String)?
    /// The completion, from a formatted percentage. Built in: "60% complete".
    public var progress: (@Sendable (String) -> String)?
    /// The items an item depends on, from a list of names. Built in: "Depends on …".
    public var dependsOn: (@Sendable (String) -> String)?
    /// The value of a parent whose children are shown. Built in: "Expanded".
    public var expanded: String?
    /// The value of a parent whose children are hidden. Built in: "Collapsed".
    public var collapsed: String?
    /// The caption of a parent. Built in: "Hide 2 sub-items" or "Show 2 sub-items".
    public var children: (@Sendable (_ count: Int, _ isExpanded: Bool) -> String)?
    /// A VoiceOver action of a bar. Built in: "Move a day earlier".
    public var moveEarlier: String?
    /// A VoiceOver action of a bar. Built in: "Move a day later".
    public var moveLater: String?
    /// A VoiceOver action of a bar. Built in: "Start a day earlier".
    public var startEarlier: String?
    /// A VoiceOver action of a bar. Built in: "Start a day later".
    public var startLater: String?
    /// A VoiceOver action of a bar. Built in: "End a day earlier".
    public var endEarlier: String?
    /// A VoiceOver action of a bar. Built in: "End a day later".
    public var endLater: String?
    /// The visible window and a span of dates. Built in: "Jan 5, 2026 – Apr 5, 2026".
    public var range: (@Sendable (_ from: String, _ to: String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - timeline: The spoken name of the chart and the list.
    ///   - rowHeader: The heading of the row-label column.
    ///   - zoom: The spoken name of the zoom menu.
    ///   - zoomName: The name of a zoom level.
    ///   - earlier: The spoken name of the backward arrow.
    ///   - later: The spoken name of the forward arrow.
    ///   - today: The Today button and the label of the today line.
    ///   - view: The spoken name of the chart and list switch.
    ///   - chart: The chart choice of the switch.
    ///   - list: The list choice of the switch.
    ///   - milestones: The label of the milestone lane and the heading of the milestones in the list.
    ///   - hint: The visible hint above an editable chart.
    ///   - bar: What VoiceOver reads for a bar, from the label, the formatted first and last days and the formatted progress.
    ///   - conflict: A conflict in words, from the app's message.
    ///   - milestone: What VoiceOver reads for a milestone.
    ///   - ended: The row text of an item that ended before the window.
    ///   - starts: The row text of an item that starts after the window.
    ///   - noDates: The row text of an item without dates.
    ///   - unscheduled: The heading of the items without dates, from their number.
    ///   - changed: Announced after a change.
    ///   - changeDates: The button of a list row that opens the date sheet.
    ///   - startDate: The first date picker of the date sheet.
    ///   - endDate: The second date picker of the date sheet.
    ///   - save: The confirm button of the date sheet.
    ///   - cancel: The dismiss button of the date sheet.
    ///   - duration: The length of an item in days.
    ///   - progress: The completion, from a formatted percentage.
    ///   - dependsOn: The items an item depends on, from a list of names.
    ///   - expanded: The value of a parent whose children are shown.
    ///   - collapsed: The value of a parent whose children are hidden.
    ///   - children: The caption of a parent.
    ///   - moveEarlier: A VoiceOver action of a bar.
    ///   - moveLater: A VoiceOver action of a bar.
    ///   - startEarlier: A VoiceOver action of a bar.
    ///   - startLater: A VoiceOver action of a bar.
    ///   - endEarlier: A VoiceOver action of a bar.
    ///   - endLater: A VoiceOver action of a bar.
    ///   - range: The visible window and a span of dates.
    public init(
        timeline: String? = nil,
        rowHeader: String? = nil,
        zoom: String? = nil,
        zoomName: (@Sendable (MonarchGanttTimelineZoom) -> String)? = nil,
        earlier: String? = nil,
        later: String? = nil,
        today: String? = nil,
        view: String? = nil,
        chart: String? = nil,
        list: String? = nil,
        milestones: String? = nil,
        hint: String? = nil,
        bar: (@Sendable (_ label: String, _ start: String, _ end: String, _ progress: String?) -> String)? = nil,
        conflict: (@Sendable (String) -> String)? = nil,
        milestone: (@Sendable (_ label: String, _ date: String, _ isDone: Bool) -> String)? = nil,
        ended: (@Sendable (String) -> String)? = nil,
        starts: (@Sendable (String) -> String)? = nil,
        noDates: String? = nil,
        unscheduled: (@Sendable (Int) -> String)? = nil,
        changed: (@Sendable (_ label: String, _ start: String, _ end: String) -> String)? = nil,
        changeDates: String? = nil,
        startDate: String? = nil,
        endDate: String? = nil,
        save: String? = nil,
        cancel: String? = nil,
        duration: (@Sendable (Int) -> String)? = nil,
        progress: (@Sendable (String) -> String)? = nil,
        dependsOn: (@Sendable (String) -> String)? = nil,
        expanded: String? = nil,
        collapsed: String? = nil,
        children: (@Sendable (_ count: Int, _ isExpanded: Bool) -> String)? = nil,
        moveEarlier: String? = nil,
        moveLater: String? = nil,
        startEarlier: String? = nil,
        startLater: String? = nil,
        endEarlier: String? = nil,
        endLater: String? = nil,
        range: (@Sendable (_ from: String, _ to: String) -> String)? = nil
    ) {
        self.timeline = timeline
        self.rowHeader = rowHeader
        self.zoom = zoom
        self.zoomName = zoomName
        self.earlier = earlier
        self.later = later
        self.today = today
        self.view = view
        self.chart = chart
        self.list = list
        self.milestones = milestones
        self.hint = hint
        self.bar = bar
        self.conflict = conflict
        self.milestone = milestone
        self.ended = ended
        self.starts = starts
        self.noDates = noDates
        self.unscheduled = unscheduled
        self.changed = changed
        self.changeDates = changeDates
        self.startDate = startDate
        self.endDate = endDate
        self.save = save
        self.cancel = cancel
        self.duration = duration
        self.progress = progress
        self.dependsOn = dependsOn
        self.expanded = expanded
        self.collapsed = collapsed
        self.children = children
        self.moveEarlier = moveEarlier
        self.moveLater = moveLater
        self.startEarlier = startEarlier
        self.startLater = startLater
        self.endEarlier = endEarlier
        self.endLater = endLater
        self.range = range
    }
}
