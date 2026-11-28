/// Built-in strings of the run timeline, from the `MonarchAiAgentRunTimeline` strings table.
enum MonarchAiAgentRunTimelineStrings {
    private static let table = "MonarchAiAgentRunTimeline"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiAgentRunTimeline.\(key)", table: table)
    }

    static var label: String { string("label") }
    static var modeLabel: String { string("modeLabel") }
    static var events: String { string("events") }
    static var totalDuration: String { string("totalDuration") }
    static var inputTokens: String { string("inputTokens") }
    static var outputTokens: String { string("outputTokens") }
    static var cost: String { string("cost") }
    static var start: String { string("start") }
    static var duration: String { string("duration") }
    static var eventColumn: String { string("eventColumn") }
    static var kindColumn: String { string("kindColumn") }
    static var running: String { string("running") }
    static var empty: String { string("empty") }
    static var notAvailable: String { string("notAvailable") }
    static var openPart: String { string("openPart") }

    /// The name of a view.
    static func mode(_ mode: MonarchAiAgentRunTimelineMode) -> String { string("mode.\(mode.rawValue)") }

    /// The name of a kind.
    static func kind(_ kind: MonarchAiRunEventKind) -> String { string("kind.\(kind.rawValue)") }

    /// A duration under a second, from the formatted milliseconds.
    static func milliseconds(_ value: String) -> String {
        MonarchStrings.string("monarch.aiAgentRunTimeline.milliseconds", table: table, value)
    }

    /// A duration under a minute, from the formatted seconds.
    static func seconds(_ value: String) -> String {
        MonarchStrings.string("monarch.aiAgentRunTimeline.seconds", table: table, value)
    }

    /// A duration in whole minutes.
    static func minutes(_ minutes: String) -> String {
        MonarchStrings.string("monarch.aiAgentRunTimeline.minutes", table: table, minutes)
    }

    /// A duration in minutes and seconds.
    static func minutesSeconds(_ minutes: String, _ seconds: String) -> String {
        MonarchStrings.string("monarch.aiAgentRunTimeline.minutesSeconds", table: table, minutes, seconds)
    }

    /// An offset from the start of the run.
    static func offset(_ duration: String) -> String {
        MonarchStrings.string("monarch.aiAgentRunTimeline.offset", table: table, duration)
    }

    /// A name and its value, read as one.
    static func fact(_ name: String, _ value: String) -> String {
        MonarchStrings.string("monarch.aiAgentRunTimeline.fact", table: table, name, value)
    }
}
