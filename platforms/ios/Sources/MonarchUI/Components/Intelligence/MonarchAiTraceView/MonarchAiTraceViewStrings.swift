/// Built-in strings of the trace view, from the `MonarchAiTraceView` strings table.
enum MonarchAiTraceViewStrings {
    private static let table = "MonarchAiTraceView"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiTraceView.\(key)", table: table)
    }

    static var label: String { string("label") }
    static var filterLabel: String { string("filterLabel") }
    static var totalDuration: String { string("totalDuration") }
    static var totalTokens: String { string("totalTokens") }
    static var totalCost: String { string("totalCost") }
    static var errors: String { string("errors") }
    static var notAvailable: String { string("notAvailable") }
    static var empty: String { string("empty") }
    static var noErrors: String { string("noErrors") }
    static var closeDetails: String { string("closeDetails") }
    static var openPart: String { string("openPart") }
    static var start: String { string("start") }
    static var end: String { string("end") }
    static var duration: String { string("duration") }
    static var inputTokens: String { string("inputTokens") }
    static var outputTokens: String { string("outputTokens") }
    static var model: String { string("model") }
    static var statusName: String { string("statusName") }
    static var kindName: String { string("kindName") }
    static var error: String { string("error") }
    static var attributes: String { string("attributes") }
    static var input: String { string("input") }
    static var output: String { string("output") }

    static func filter(_ filter: MonarchAiTraceFilter) -> String { string("filter.\(filter.rawValue)") }
    static func kind(_ kind: MonarchAiTraceSpanKind) -> String { string("kind.\(kind.rawValue)") }
    static func status(_ status: MonarchAiTraceSpanStatus) -> String { string("status.\(status.rawValue)") }

    /// "+1.2 s".
    static func offset(_ duration: String) -> String {
        MonarchStrings.string("monarch.aiTraceView.offset", table: table, duration)
    }

    /// "Starts at +1.2 s, takes 850 ms".
    static func timing(_ start: String, _ duration: String) -> String {
        MonarchStrings.string("monarch.aiTraceView.timing", table: table, start, duration)
    }

    /// "Starts at +1.2 s, still running".
    static func timingRunning(_ start: String) -> String {
        MonarchStrings.string("monarch.aiTraceView.timingRunning", table: table, start)
    }

    static func expand(_ name: String) -> String {
        MonarchStrings.string("monarch.aiTraceView.expand", table: table, name)
    }

    static func collapse(_ name: String) -> String {
        MonarchStrings.string("monarch.aiTraceView.collapse", table: table, name)
    }
    /// The Span column heading.
    static var columnSpan: String { string("column.span") }
    /// The Timeline column heading.
    static var columnTimeline: String { string("column.timeline") }
    /// The Duration column heading.
    static var columnDuration: String { string("duration") }
}
