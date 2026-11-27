/// Built-in strings of ``MonarchGanttTimeline``, from the `MonarchGanttTimeline` strings table.
enum MonarchGanttTimelineStrings {
    private static let table = "MonarchGanttTimeline"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.ganttTimeline.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    private static func plural(_ key: String, _ count: Int) -> String {
        count == 1 ? string("\(key).one") : string("\(key).other", count)
    }

    static var timeline: String { string("timeline") }
    static var rowHeader: String { string("rowHeader") }
    static var zoom: String { string("zoom") }
    static func zoomName(_ zoom: MonarchGanttTimelineZoom) -> String { string("zoom.\(zoom.rawValue)") }
    static var earlier: String { string("earlier") }
    static var later: String { string("later") }
    static var today: String { string("today") }
    static var view: String { string("view") }
    static var chart: String { string("chart") }
    static var list: String { string("list") }
    static var milestones: String { string("milestones") }
    static var hint: String { string("hint") }
    static func bar(_ label: String, _ start: String, _ end: String) -> String { string("bar", label, start, end) }
    static func barDay(_ label: String, _ day: String) -> String { string("barDay", label, day) }
    static func progress(_ percent: String) -> String { string("progress", percent) }
    static func conflict(_ message: String) -> String { string("conflict", message) }
    static func milestone(_ label: String, _ date: String, isDone: Bool) -> String {
        string(isDone ? "milestoneDone" : "milestone", label, date)
    }
    static func ended(_ date: String) -> String { string("ended", date) }
    static func starts(_ date: String) -> String { string("starts", date) }
    static var noDates: String { string("noDates") }
    static func unscheduled(_ count: Int) -> String { plural("unscheduled", count) }
    static func changed(_ label: String, _ start: String, _ end: String) -> String {
        start == end ? string("changedDay", label, start) : string("changed", label, start, end)
    }
    static var changeDates: String { string("changeDates") }
    static var startDate: String { string("startDate") }
    static var endDate: String { string("endDate") }
    static var save: String { string("save") }
    static var cancel: String { string("cancel") }
    static func duration(_ days: Int) -> String { plural("duration", days) }
    static func dependsOn(_ names: String) -> String { string("dependsOn", names) }
    static var expanded: String { string("expanded") }
    static var collapsed: String { string("collapsed") }
    static func children(_ count: Int, isExpanded: Bool) -> String {
        plural(isExpanded ? "hideChildren" : "showChildren", count)
    }
    static var moveEarlier: String { string("moveEarlier") }
    static var moveLater: String { string("moveLater") }
    static var startEarlier: String { string("startEarlier") }
    static var startLater: String { string("startLater") }
    static var endEarlier: String { string("endEarlier") }
    static var endLater: String { string("endLater") }
    static func range(_ from: String, _ to: String) -> String { string("range", from, to) }
}
