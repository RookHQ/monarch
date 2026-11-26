/// Built-in strings of the sub-agent tree, from the `MonarchAiSubagentTree` strings table.
enum MonarchAiSubagentTreeStrings {
    private static let table = "MonarchAiSubagentTree"

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        let format = MonarchStrings.string("monarch.aiSubagentTree.\(key)", table: table)
        return arguments.isEmpty ? format : String(format: format, arguments: arguments)
    }

    static var label: String { string("label") }
    static var empty: String { string("empty") }
    static var stop: String { string("stop") }
    static var open: String { string("open") }
    static var respond: String { string("respond") }
    static var openPart: String { string("openPart") }

    /// The name of a status.
    static func status(_ status: MonarchAiSubagentStatus) -> String { string("status.\(status.rawValue)") }

    /// "1 agent" or "4 agents", from the formatted count.
    static func agentCount(_ count: Int, _ formatted: String) -> String {
        string(count == 1 ? "agentCount.one" : "agentCount.other", formatted)
    }

    /// One part of the header breakdown, from the formatted count.
    static func statusCount(_ status: MonarchAiSubagentStatus, _ count: Int, _ formatted: String) -> String {
        let plural = count == 1 ? "one" : "other"
        return string("statusCount.\(status.rawValue).\(plural)", formatted)
    }

    /// The header: the total and the breakdown.
    static func summary(_ agents: String, _ breakdown: String) -> String { string("summary", agents, breakdown) }

    /// "3 of 7 steps".
    static func stepsOf(_ done: String, _ total: String) -> String { string("stepsOf", done, total) }

    /// "1 step" or "3 steps".
    static func steps(_ count: Int, _ formatted: String) -> String {
        string(count == 1 ? "steps.one" : "steps.other", formatted)
    }

    /// "1 tool call" or "12 tool calls".
    static func toolCalls(_ count: Int, _ formatted: String) -> String {
        string(count == 1 ? "toolCalls.one" : "toolCalls.other", formatted)
    }

    static func started(_ time: String) -> String { string("started", time) }
    static func ran(_ start: String, _ end: String) -> String { string("ran", start, end) }
    static func actionsFor(_ name: String) -> String { string("actionsFor", name) }
    static func announce(_ name: String, _ status: String) -> String { string("announce", name, status) }
    static func expand(_ name: String) -> String { string("expand", name) }
    static func collapse(_ name: String) -> String { string("collapse", name) }
}
